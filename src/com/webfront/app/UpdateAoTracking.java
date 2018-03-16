/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import static asjava.uniobjects.UniObjectsTokens.LOCK_NO_LOCK;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import com.webfront.exception.NotFoundException;
import com.webfront.exception.RecordLockException;
import com.webfront.u2.model.DynArray;
import com.webfront.u2.model.Result;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class UpdateAoTracking extends BaseApp {

    public String[] dateTime;
    public UvData aoTrackingRec;
    public UvData aoRec;
    public UvData orderRec;
    boolean isHist;
    boolean isReleased;
    boolean isNew;

    @Override
    public boolean mainLoop() {
        dateTime = getDateTime();
        String timeStamp = dateTime[0] + "." + dateTime[1];
        String maOrderId;
        String storeId;
        String divId;
        String orkId;
        String srpExchRate;
        String ibvExchRate;
        String vendorOrderId;
        String orderType;
        String aotId;

        if (listName.isEmpty()) {
            progress.display("SELECTLIST or AO ids is required");
            teardown();
            return false;
        }

        try {
            UniSelectList list3 = readSession.selectList(3);
            list3.getList(listName);
            UniDynArray temp = list3.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            UniSelectList list = getList(readSession, listName);
            while (!list.isLastRecordRead()) {
                String aoId = list.next().toString();
                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                if (aoId.isEmpty()) {
                    continue;
                }
                try {
                    aoRec = getAo(aoId);
                    progress.display(aoId);
                    orderRec = new UvData();
                    isReleased = false;
                    maOrderId = aoRec.getData().extract(30).toString();
                    storeId = aoRec.getData().extract(157, 1).toString();
                    divId = aoRec.getData().extract(158, 1).toString();
                    orkId = aoRec.getData().extract(197).toString();
                    srpExchRate = aoRec.getData().extract(367, 1).toString();
                    ibvExchRate = aoRec.getData().extract(366, 1).toString();
                    vendorOrderId = aoRec.getData().extract(165, 1).toString();
                    orderType = aoId.split("\\*")[2];
                    aotId = vendorOrderId + "*" + divId;
                    if (!maOrderId.isEmpty()) {
                        try {
                            orderRec = getOrder(maOrderId);
                        } catch (NotFoundException e) {
                            progress.display(e.getMessage());
                        }
                    }
                    try {
                        isNew = getAoTracking(aotId);
                        if (!aoTrackingRec.getData().extract(23).toString().equals(timeStamp)) {
                            isNew = true;
                            aoTrackingRec.setData(new UniDynArray());
                        }
                        if (isNew) {
                            aoTrackingRec.getData().replace(10, srpExchRate);
                            aoTrackingRec.getData().replace(11, ibvExchRate);
                            aoTrackingRec.getData().replace(16, aoRec.getData().extract(290));
                            aoTrackingRec.getData().replace(17, aoRec.getData().extract(204));
                            aoTrackingRec.getData().replace(23, timeStamp);
                        }
                        updateAoTracking();
                    } catch (RecordLockException ex) {
                        Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (NotFoundException e) {
                    progress.display(e.getMessage());
                }
            }
        } catch (UniSessionException ex) {
            Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    private UvData getAo(String aoId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(aoId);
        isHist = false;
        int result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.getRecord(readFiles.get("AOP.HIST"), uvData);
            if (result == UVE_RNF) {
                result = FileUtils.getRecord(readFiles.get("AOP.HIST.ARCHIVE"), uvData);
                if (result == UVE_RNF) {
                    throw new NotFoundException(aoId + " not found");
                }
            }
        }
        isHist = true;
        return uvData;
    }

    private UvData getOrder(String orderId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result;
        result = FileUtils.getRecord(readFiles.get("ORDERS.RELEASE"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.getRecord(readFiles.get("ORDER.REL.ERROR"), uvData);
            if (result == UVE_RNF) {
                isReleased = true;
                result = FileUtils.getRecord(readFiles.get("ORDERS"), uvData);
                if (result == UVE_RNF) {
                    result = FileUtils.getRecord(readFiles.get("ORDERS.HISTORY"), uvData);
                    if (result == UVE_RNF) {
                        throw new NotFoundException("Order# " + orderId + " not found");
                    }
                }
            }
        }
        return uvData;
    }

    private boolean getAoTracking(String aoTrackingId) throws RecordLockException {
        aoTrackingRec = new UvData();
        aoTrackingRec.setId(aoTrackingId);
        int result;
        result = FileUtils.lockRecord(readFiles.get("AO.TRACKING"), aoTrackingRec);
        if (result == LOCK_NO_LOCK) {
            throw new RecordLockException("AO.TRACKING lock error " + aoTrackingId);
        }
        result = FileUtils.getRecord(readFiles.get("AO.TRACKING"), aoTrackingRec);
        return (result == UVE_RNF);
    }

    private void updateAoTracking() {
        UniDynArray aot = aoTrackingRec.getData();
        UniDynArray ao = aoRec.getData();
        String aoId = aoRec.getId();
        String orkId = ao.extract(197).toString();
        String orderType = aoId.split("\\*")[2];
        String batchSeq = orkId.split("\\*")[3];
        boolean canRelease = (batchSeq.equals("03") || batchSeq.equals("02"));
        int orderCount = aot.dcount(2);
        Result aoResult = DynArray.locate(aot, aoId, 2, "ar");
        int aoPosition = aoResult.location;
        String orderDate = "";
        if (!ao.extract(30).toString().isEmpty()) {
            orderDate = orderRec.getData().extract(1).toString();
        }

        if (aoResult.isSuccess || isNew) {
            aot.replace(2, aoPosition, aoId);
            aot.replace(3, aoPosition, ao.extract(26));
            aot.replace(4, aoPosition, orkId);
            aot.replace(5, aoPosition, ao.extract(30).toString());
            aot.replace(6, aoPosition, ao.extract(3).toString());
            if (isReleased) {
                aot.replace(7, aoPosition, orderDate);
            } else {
                aot.replace(7, aoPosition, "");
            }
            aot.replace(8, aoPosition, isReleased ? "1" : "0");
            aot.replace(13, aoPosition, DynArray.sum(ao, 371));
            aot.replace(18, aoPosition, DynArray.sum(ao, 173));
            isNew = false;
        } else {
            if (orderType.equals("NEG")) {
                aoPosition = aot.dcount(2) + 1;
            }
            aot.insert(2, aoPosition, aoId);
            aot.insert(3, aoPosition, ao.extract(26));
            aot.insert(4, aoPosition, orkId);
            aot.insert(5, aoPosition, ao.extract(30).toString());
            aot.insert(6, aoPosition, ao.extract(3).toString());
            if (isReleased) {
                aot.insert(7, aoPosition, orderDate);
            } else {
                aot.insert(7, aoPosition, "");
            }
            aot.insert(8, aoPosition, isReleased ? "1" : "0");
            aot.insert(13, aoPosition, DynArray.sum(ao, 371));
            aot.insert(18, aoPosition, DynArray.sum(ao, 173));
        }

        aot.replace(1, DynArray.sum(aot, 3));
        aot.replace(12, DynArray.sum(aot, 13));
        aoTrackingRec.setData(aot);
        int result = FileUtils.writeRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
        if (result == -1) {
            result = FileUtils.unlockRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
            progress.display("Error writing AO.TRACKING " + aoTrackingRec.getId());
        }
    }

}
