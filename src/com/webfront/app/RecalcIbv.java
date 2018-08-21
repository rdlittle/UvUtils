/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniStringException;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import static asjava.uniobjects.UniObjectsTokens.LOCK_NO_LOCK;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
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
public class RecalcIbv extends BaseApp {

    public String[] dateTime;
    public UvData aoTrackingRec;
    public UvData aoRec;
    public UvData orderRec;
    UniSubroutine subroutine;
    boolean isArchive;
    boolean isReleased;
    boolean isNew;
    int svrStatus;
    String svrCtrlCode;
    String svrMessage;
    String aoFileName;
    String orderFileName;
    String ibvFileName;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST or AFFILIATE.ORDERS ids is required");
            teardown();
            return false;
        }
        String aoId;
        String maOrderId;
        aoFileName = "";
        orderFileName = "";
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
                aoId = recList.extract(attr).toString();
                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                progress.display(aoId);
                if (aoId.isEmpty()) {
                    continue;
                }
                UvData aoRec = null;
                UvData orderRec = null;
                UvData ordersDetailRec = null;
                
                // get AFFILIATE.ORDERS/AOP.HIST rec
                try {
                    aoRec = getAo(aoId);
                } catch (NotFoundException ex) {
                    progress.display("AFFILIATE.ORDERS " + aoId);
                } catch (RecordLockException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (aoRec.getData() == null) {
                    progress.display("AFFILIATE.ORDERS " + aoId + " is null");
                    releaseAo(aoRec);
                    continue;
                }
                maOrderId = aoRec.getData().extract(30).toString();
                if (maOrderId.isEmpty()) {
                    progress.display("AFFILIATE.ORDERS<30> is empty" + aoId);
                    releaseAo(aoRec);
                    continue;
                }

                // get ORDERS/ORDERS.RELEASE rec
                try {
                    orderRec = getOrder(maOrderId);
                } catch (NotFoundException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                    progress.display("ORDER " + maOrderId + " not found");
                    releaseAo(aoRec);
                } catch (RecordLockException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                    progress.display("ORDER " + maOrderId + " lock error");
                    releaseAo(aoRec);
                }

                // get ORDERS.DETAIL rec
                try {
                    ordersDetailRec = getOrdersDetail(maOrderId);
                } catch (NotFoundException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RecordLockException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                }

                UniDynArray ibv;
                try {
                    ibv = recalcIbv(aoRec);
                    if (ibv == null) {
                        progress.display("IBV recalc error");
                        releaseAo(aoRec);
                        releaseOrder(orderRec);
                        continue;
                    }
                    int placementCount = orderRec.getData().extract(152).dcount(1);
                    UniDynArray uda = aoRec.getData();

                    uda.replace(149, ibv);
                    aoRec.setData(uda);
                    uda = orderRec.getData();
                    int ord58Count = uda.dcount(58);
                    //uda.replace(58, ord58Count + 1, "Correction of IBV due to system error");
                    uda.replace(149, ibv);
                    if (placementCount == 1) {
                        uda.replace(152, SysUtils.sum(ibv));
                    } else {
                        UniDynArray ord152 = calcIbvSplit(orderRec, SysUtils.sum(ibv));
                        uda.replace(152, ord152);
                    }
                    orderRec.setData(uda);
                    if (ordersDetailRec != null) {
                        uda = ordersDetailRec.getData();
                        uda.replace(6, SysUtils.sum(ibv));
                        ordersDetailRec.setData(uda);
                    }
                } catch (UniSubroutineException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                    progress.display("subroutine exception " + ex);
                    releaseAo(aoRec);
                    releaseOrder(orderRec);
                    releaseOrdersDetail(ordersDetailRec);
                }
                writeAo(aoRec);
                writeOrder(orderRec);
                writeOrdersDetail(ordersDetailRec);
                updateIbv(orderRec);
            }
            releaseAo(aoRec);
            releaseOrder(orderRec);
        } catch (UniSessionException ex) {
            Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
            releaseAo(aoRec);
            releaseOrder(orderRec);
        } catch (UniSelectListException ex) {
            Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
            releaseAo(aoRec);
            releaseOrder(orderRec);
        }
        teardown();
        return true;
    }

    private UvData getAo(String aoId) throws NotFoundException, RecordLockException {
        UvData uvData = new UvData();
        uvData.setId(aoId);
        isReleased = false;
        isArchive = false;
        aoFileName = "AFFILIATE.ORDERS";
        int result = FileUtils.lockRecord(writeFiles.get("AFFILIATE.ORDERS"), uvData);
        if (result == LOCK_NO_LOCK) {
            throw new RecordLockException("AFFILIATE.ORDERS lock error " + aoId);
        }
        if (result == UVE_RNF) {
            isReleased = true;
            aoFileName = "AOP.HIST";
            result = FileUtils.getRecord(writeFiles.get("AOP.HIST"), uvData);
            if (result == UVE_RNF) {
                result = FileUtils.getRecord(writeFiles.get("AOP.HIST.ARCHIVE"), uvData);
                isArchive = true;
                if (result == UVE_RNF) {
                    aoFileName = null;
                    throw new NotFoundException(aoId + " not found");
                }
            }
        }
        return uvData;
    }

    private UvData getOrder(String orderId) throws NotFoundException, RecordLockException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result;
        orderFileName = "ORDERS.HISTORY";
        if (isArchive) {
            result = FileUtils.lockRecord(writeFiles.get("ORDERS.HISTORY"), uvData);
            if (result == UVE_RNF) {
                orderFileName = null;
                throw new NotFoundException("Order# " + orderId + " not found");
            }
        } else if (isReleased) {
            orderFileName = "ORDERS";
            result = FileUtils.lockRecord(writeFiles.get("ORDERS"), uvData);
            if (result == UVE_RNF) {
                result = FileUtils.lockRecord(writeFiles.get("ORDERS.HISTORY"), uvData);
                if (result == UVE_RNF) {
                    orderFileName = null;
                    throw new NotFoundException("Order# " + orderId + " not found");
                }
            }
        } else {
            orderFileName = "ORDERS.RELEASE";
            result = FileUtils.lockRecord(writeFiles.get("ORDERS.RELEASE"), uvData);
            if (result == UVE_RNF) {
                orderFileName = "ORDER.REL.ERROR";
                result = FileUtils.lockRecord(writeFiles.get("ORDER.REL.ERROR"), uvData);
                if (result == UVE_RNF) {
                    isReleased = true;
                    orderFileName = "ORDERS";
                    result = FileUtils.lockRecord(writeFiles.get("ORDERS"), uvData);
                    if (result == UVE_RNF) {
                        orderFileName = "ORDERS.HISTORY";
                        result = FileUtils.lockRecord(writeFiles.get("ORDERS.HISTORY"), uvData);
                        if (result == UVE_RNF) {
                            orderFileName = null;
                            throw new NotFoundException("Order# " + orderId + " not found");
                        }
                    }
                }
            }
        }
        return uvData;
    }

    private UvData getOrdersDetail(String orderId) throws NotFoundException, RecordLockException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result = FileUtils.lockRecord(writeFiles.get("ORDERS.DETAIL"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.unlockRecord(writeFiles.get("ORDERS.DETAIL"), uvData);
            return null;
        }
        return uvData;
    }

    UniDynArray calcIbvSplit(UvData orderRec, int ibv) {
        UniDynArray uda = orderRec.getData().extract(152);
        try {
            uda.replace(2, Integer.toString(ibv));
            UniDynArray iList = new UniDynArray();
            UniDynArray oList = new UniDynArray();
            UniDynArray eList = new UniDynArray();

            iList = uda;
            subroutine = readSession.subroutine("getIbvSplit", 3);
            subroutine.setArg(0, uda);
            subroutine.setArg(1, oList);
            subroutine.setArg(2, eList);
            subroutine.call();
            uda = subroutine.getArgDynArray(1);
        } catch (UniSessionException ex) {
            Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSubroutineException ex) {
            Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
        }
        return uda;
    }

    UniDynArray recalcIbv(UvData aoRec) throws UniSessionException, UniSubroutineException {
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();
        String aggId = aoRec.getData().extract(181, 1).toString();
        String storeId = aoRec.getData().extract(157, 1).toString();
        String ppcId = aoRec.getData().extract(8).toString();
        String orderDate = aoRec.getData().extract(161, 1).toString();

        iList.replace(4, aggId);
        iList.replace(5, storeId);
        iList.replace(6, ppcId);
        iList.replace(7, orderDate);
        iList.replace(8, aoRec.getData().extract(371));
        iList.replace(9, aoRec.getData().extract(173));
        iList.replace(10, aoRec.getData().extract(180));

        subroutine = readSession.subroutine("getCashbackCalc.uvs", 3);
        subroutine.setArg(0, iList);
        subroutine.setArg(1, oList);
        subroutine.setArg(2, eList);
        subroutine.call();
        eList = subroutine.getArgDynArray(2);
        svrStatus = Integer.parseInt(eList.extract(1).toString());
        svrCtrlCode = eList.extract(5).toString();
        svrMessage = eList.extract(2).toString();
        if (svrStatus == -1) {
            return null;
        }
        oList = subroutine.getArgDynArray(1);
        return oList.extract(3);
    }

    void releaseAo(UvData rec) {
        if (aoFileName == null) {
            return;
        }
        if (rec == null) {
            return;
        }
        FileUtils.unlockRecord(writeFiles.get(aoFileName), rec);
    }

    void releaseOrder(UvData rec) {
        if (orderFileName == null) {
            return;
        }
        if (rec == null) {
            return;
        }
        FileUtils.unlockRecord(writeFiles.get(orderFileName), rec);
    }

    void releaseOrdersDetail(UvData rec) {
        if (rec != null) {
            FileUtils.unlockRecord(writeFiles.get("ORDERS.DETAIL"), rec);
        }
    }

    void updateIbv(UvData orderRec) {
        UniDynArray placements = orderRec.getData().extract(151);
        int placementCount = placements.dcount(1);
        for (int i = 1; i <= placementCount; i++) {
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
                uda.replace(3, r.location, placementAmount);
                ibvRec.setData(uda);
            }
            result = FileUtils.writeRecord(writeFiles.get(ibvFileName), ibvRec);
            result = FileUtils.unlockRecord(writeFiles.get(ibvFileName), ibvRec);
        }
    }

    void writeAo(UvData aoRec) {
        int result = FileUtils.writeRecord(writeFiles.get(aoFileName), aoRec);
    }

    void writeOrder(UvData orderRec) {
        int result = FileUtils.writeRecord(writeFiles.get(orderFileName), orderRec);
    }

    void writeOrdersDetail(UvData ordersDetailRec) {
        if (ordersDetailRec != null) {
            int result = FileUtils.writeRecord(writeFiles.get("ORDERS.DETAIL"), ordersDetailRec);
        }
    }
}
