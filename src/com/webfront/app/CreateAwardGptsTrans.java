/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniException;
import asjava.uniclientlibs.UniStringException;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.exception.NotFoundException;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import com.webfront.util.SysUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class CreateAwardGptsTrans extends BaseApp {

    String maOrderId;
    String orderFileName;
    boolean isArchive;
    boolean isReleased;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST or ORDERS ids is required");
            teardown();
            return false;
        }

        try {
            UniSelectList list = readSession.selectList(3);
            list.getList(listName);
            UniDynArray temp = list.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            list = readSession.selectList(0);
            list.getList(listName);
            UniDynArray recList = list.readList();
            for (int attr = 1; attr <= itemCount; attr++) {
                maOrderId = recList.extract(attr).toString();
                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                progress.display(maOrderId);
                if (maOrderId.isEmpty()) {
                    continue;
                }
                UvData orderRec = null;
                UvData ordersDetailRec = null;

                // get ORDERS/ORDERS.RELEASE rec
                try {
                    orderRec = getOrder(maOrderId);
                } catch (NotFoundException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                    progress.display("ORDER " + maOrderId + " not found");
                    continue;
                }

                // get ORDERS.DETAIL rec
                try {
                    ordersDetailRec = getOrdersDetail(maOrderId);
                } catch (NotFoundException ex) {
                    progress.display("ORDERS.DETAIL " + maOrderId + " not found");
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }

                try {
                    // First call makes the pending transaction
                    createGptsTrans(orderRec, ordersDetailRec);
                    // Second call turns the pending transaction into an awarded transaction
                    createGptsTrans(orderRec, ordersDetailRec);
                } catch (UniException ex) {
                    progress.display(ex.getMessage());
                    Logger.getLogger(CreateAwardGptsTrans.class.getName()).log(Level.SEVERE, null, ex);
                }

                // Rebuild GPTS.CUST.SUMMARY
                try {
                    String pcId = orderRec.getData().extract(8).toString();
                    progress.display("Rebuild summary "+pcId);
                    if (pcId.startsWith("C")) {
                        pcId.substring(1);
                        String hcc = getHomeCountry(pcId);
                        if (hcc != "") {
                            rebuildSummary(pcId);
                        }
                    }
                } catch (UniException ex) {
                    progress.display(ex.getMessage());
                    Logger.getLogger(CreateAwardGptsTrans.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NotFoundException ex) {
                    progress.display(ex.getMessage());
                    Logger.getLogger(CreateAwardGptsTrans.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        } catch (UniSessionException ex) {
            progress.display(ex.getMessage());
            Logger.getLogger(CreateAwardGptsTrans.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            progress.display(ex.getMessage());
            Logger.getLogger(CreateAwardGptsTrans.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    private UvData getOrder(String orderId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result;
        orderFileName = "ORDERS";
        result = FileUtils.getRecord(readFiles.get(orderFileName), uvData);
        if (result == UVE_RNF) {
            throw new NotFoundException("ORDERS " + orderId + " not found");
        }
        return uvData;
    }

    private UvData getOrdersDetail(String orderId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result = FileUtils.getRecord(readFiles.get("ORDERS.DETAIL"), uvData);
        if (result == UVE_RNF) {
            throw new NotFoundException("ORDERS.DETAIL " + orderId + " not found");
        }
        return uvData;
    }

    private void createGptsTrans(UvData orderRec, UvData ordersDetailRec) throws UniSessionException, UniSubroutineException, UniException {
        String cbAmount = orderRec.getData().extract(375).toString();
        if (cbAmount == null || cbAmount.isEmpty() || cbAmount == "") {
            cbAmount = "0";
        }
        int cb = Integer.parseInt(cbAmount);
        cbAmount = Integer.toString(Math.abs(cb));

        String srpAmount = orderRec.getData().extract(26).toString();
        if (srpAmount == null || srpAmount == "") {
            srpAmount = "0";
        }
        Integer srp = Integer.parseInt(srpAmount);
        srpAmount = Integer.toString(Math.abs(srp));

        int ibv = SysUtils.sum(orderRec.getData().extract(149));
        String ibvAmount = Integer.toString(ibv);
        String isRefund = cb < 0 ? "1" : "0";
        String orderRef = orderRec.getData().extract(2).toString();
        String aggId = orderRec.getData().extract(181, 1).toString();
        String storeId = orderRec.getData().extract(157, 1).toString();
        String storeName = ordersDetailRec.getData().extract(12, 1).toString();
        String affiliateStoreName = aggId + "*" + storeId + "*" + storeName;
        if (orderRef.startsWith("R")) {
            try {
                orderRef = readSession.oconv(orderRef, "mcn").toString();
            } catch (UniStringException ex) {
                Logger.getLogger(CreateAwardGptsTrans.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            orderRef = "";
        }

        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();
        iList.replace(1, orderRec.getData().extract(8).toString());
        iList.replace(2, cbAmount);
        iList.replace(5, "0");
        iList.replace(6, orderRec.getData().extract(4).toString());
        iList.replace(7, orderRec.getData().extract(1).toString());
        iList.replace(10, "0");
        iList.replace(12, orderRec.getId());
        iList.replace(13, orderRec.getData().extract(158, 1).toString());
        iList.replace(15, "AV");
        iList.replace(16, "0");
        iList.replace(17, orderRec.getData().extract(290).toString());
        iList.replace(18, orderRec.getData().extract(204).toString());
        iList.replace(19, "W");
        iList.replace(20, "ENG");
        iList.replace(21, "0");
        iList.replace(22, srpAmount);
        iList.replace(23, aggId);
        iList.replace(24, srpAmount);
        iList.replace(27, isRefund);
        iList.replace(28, orderRef);
        iList.replace(29, orderRec.getData().extract(165, 1).toString());
        iList.replace(33, ibvAmount);
        iList.replace(35, affiliateStoreName);

        UniSubroutine subroutine = writeSession.subroutine("SET.GPTS.TRANS", 3);
        subroutine.setArg(0, iList);
        subroutine.setArg(1, oList);
        subroutine.setArg(2, eList);

        subroutine.call();

        eList = new UniDynArray(subroutine.getArg(2));
        int svrStatus = Integer.parseInt(eList.extract(1).toString());
        String svrMessage = eList.extract(2).toString();
        String svrCtrlCode = eList.extract(5).toString();

        if (svrStatus == -1) {
            throw new UniException(svrCtrlCode + " " + svrMessage, 1);
        }
    }

    private void rebuildSummary(String summaryId) throws UniSessionException, UniSubroutineException, UniException {
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();
        iList.replace(4, summaryId);
        UniSubroutine subroutine = writeSession.subroutine("setCashbackUtilRebuildSummary.uvs", 3);
        subroutine.setArg(0, iList);
        subroutine.setArg(1, oList);
        subroutine.setArg(2, eList);

        subroutine.call();

        eList = new UniDynArray(subroutine.getArg(2));
        int svrStatus = Integer.parseInt(eList.extract(1).toString());
        String svrMessage = eList.extract(2).toString();
        String svrCtrlCode = eList.extract(5).toString();
        if (svrStatus == -1) {
            throw new UniException(svrCtrlCode + " " + svrMessage, 1);
        }
    }

    private String getHomeCountry(String id) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(id);
        int attr = 125;
        int result = FileUtils.getRecord(readFiles.get("CUST"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.getRecord(readFiles.get("EZ.CUST"), uvData);
            if (result == UVE_RNF) {
                throw new NotFoundException("CUST " + id + " not found");
            }
            attr = 15;
        }
        String hcc = uvData.getData().extract(attr).toString();
        return hcc;
    }
}
