/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import com.webfront.exception.NotFoundException;
import com.webfront.exception.RecordLockException;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import com.webfront.util.Result;
import com.webfront.util.SysUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetIbvFromOrder extends BaseApp {

    public UvData orderRec;
    UniSubroutine subroutine;
    boolean isArchive;
    boolean isReleased;
    boolean isNew;
    int svrStatus;
    String orderFileName;
    String svrCtrlCode;
    String svrMessage;
    String ibvFileName;

    @Override
    public boolean mainLoop() {
        try {
            if (listName.isEmpty()) {
                progress.display("SELECTLIST of ORDERS/ORDERS.RELEASE ids is required");
                teardown();
                return false;
            }
            String orderId;
            orderFileName = "";
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
                orderId = recList.extract(attr).toString();
                if (orderId == null) {
                    continue;
                }

                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                progress.display(orderId);
                if (orderId.isEmpty()) {
                    continue;
                }

                UvData orderRec = null;
                // get ORDERS/ORDERS.RELEASE rec
                try {
                    orderRec = getOrder(orderId);
                } catch (NotFoundException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                    progress.display("ORDER " + orderId + " not found");
                }
                updateIbv(orderRec);
            }
        } catch (UniSelectListException ex) {
            Logger.getLogger(SetIbvFromOrder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSessionException ex) {
            Logger.getLogger(SetIbvFromOrder.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    private UvData getOrder(String orderId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result;

        orderFileName = "ORDERS.RELEASE";
        result = FileUtils.getRecord(writeFiles.get("ORDERS.RELEASE"), uvData);
        if (result == UVE_RNF) {
            orderFileName = "ORDER.REL.ERROR";
            result = FileUtils.getRecord(writeFiles.get("ORDER.REL.ERROR"), uvData);
            if (result == UVE_RNF) {
                isReleased = true;
                orderFileName = "ORDERS";
                result = FileUtils.getRecord(writeFiles.get("ORDERS"), uvData);
                if (result == UVE_RNF) {
                    orderFileName = "ORDERS.HISTORY";
                    result = FileUtils.getRecord(writeFiles.get("ORDERS.HISTORY"), uvData);
                    if (result == UVE_RNF) {
                        orderFileName = null;
                        throw new NotFoundException("Order# " + orderId + " not found");
                    }
                }
            }
        }

        return uvData;
    }

    void updateIbv(UvData orderRec) {
        UniDynArray placements = orderRec.getData().extract(151);
        int placementCount = placements.dcount(1);
        boolean isFixed;
        for (int i = 1; i <= placementCount; i++) {
            isFixed = false;
            UvData ibvRec = new UvData();
            UniDynArray uda = new UniDynArray();
            String placementId = orderRec.getData().extract(151, i).toString();
            String placementAmount = orderRec.getData().extract(152, i).toString();
            ibvRec.setId(placementId);
            String target = orderRec.getId();
            if (orderRec.getData().extract(8).toString().startsWith("C")) {
                target = "PC" + target;
            }
            ibvFileName = "IBV.PROJ";
            int result = FileUtils.lockRecord(writeFiles.get(ibvFileName), ibvRec);
            uda = ibvRec.getData();
            if (result == UVE_RNF) {
                uda = new UniDynArray();
                result = FileUtils.unlockRecord(writeFiles.get(ibvFileName), ibvRec);
            }
            Result r = SysUtils.locate(target, uda, 2);
            if (r.isSuccess) {
                if (!uda.extract(3, i).equals(placementAmount)) {
                    isFixed = true;
                }
                uda.replace(3, r.location, placementAmount);
                ibvRec.setData(uda);
            } else {
                result = FileUtils.unlockRecord(writeFiles.get(ibvFileName), ibvRec);
                ibvFileName = "IBV";
                result = FileUtils.lockRecord(writeFiles.get(ibvFileName), ibvRec);
                if (result == UVE_RNF) {
                    uda = new UniDynArray();
                    result = FileUtils.unlockRecord(writeFiles.get(ibvFileName), ibvRec);
                    progress.display("Couldn't find " + target + " in IBV/IBV.PROJ " + placementId);
                    continue;
                }
                uda = ibvRec.getData();
                r = SysUtils.locate(target, uda, 2);
                if (!r.isSuccess) {
                    result = FileUtils.unlockRecord(writeFiles.get(ibvFileName), ibvRec);
                    progress.display("Couldn't find " + target + " in IBV/IBV.PROJ " + placementId);
                    continue;
                }
                if (!uda.extract(3, i).equals(placementAmount)) {
                    isFixed = true;
                }
                uda.replace(3, r.location, placementAmount);
                ibvRec.setData(uda);
                if(isFixed) {
                    progress.display(placementId+" was fixed");
                }
            }
            result = FileUtils.writeRecord(writeFiles.get(ibvFileName), ibvRec);
            result = FileUtils.unlockRecord(writeFiles.get(ibvFileName), ibvRec);
        }
    }
}
