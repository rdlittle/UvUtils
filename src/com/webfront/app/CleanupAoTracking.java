/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_EIO;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import static asjava.uniobjects.UniObjectsTokens.LOCK_NO_LOCK;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import com.webfront.exception.NotFoundException;
import com.webfront.exception.RecordLockException;
import com.webfront.u2.model.DynArray;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class CleanupAoTracking extends BaseApp {

    public String[] dateTime;
    public UvData aoTrackingRec;
    public UvData aoRec;
    public UvData orderRec;
    boolean isArchive;
    boolean isReleased;
    boolean isNew;

    @Override
    public boolean mainLoop() {
        dateTime = getDateTime();
        String maOrderId;
        String aotId = "";

        if (listName.isEmpty()) {
            progress.display("SELECTLIST or AO.TRACKING ids is required");
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
                aotId = list.next().toString();
                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                progress.display(aotId);
                if (aotId.isEmpty()) {
                    continue;
                }
                if (getAoTracking(aotId)) {
                    if (aoTrackingRec.getData().extract(19).toString() == "1") {
                        FileUtils.unlockRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
                        continue;
                    }
                    aoTrackingRec.getData().replace(19, "1");
                    int ord;
                    int orderCount = aoTrackingRec.getData().dcount(2);
                    for (ord = orderCount; ord != 0; ord--) {
                        String aoId = aoTrackingRec.getData().extract(2, ord).toString();
                        try {
                            aoRec = getAo(aoId);
                            maOrderId = aoRec.getData().extract(30).toString();
                            if (!maOrderId.isEmpty()) {
                                orderRec = getOrder(maOrderId);
                            }
                        } catch (NotFoundException ex) {
                            updateAoTracking(ord);
                        }
                    }
                    setAoTracking();
                }
            }
        } catch (UniSessionException ex) {
            Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(UpdateAoTracking.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RecordLockException ex) {
            progress.display("Can't lock AO.TRACKING " + aotId);
        }
        teardown();
        return true;

    }

    private UvData getAo(String aoId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(aoId);
        isReleased = false;
        isArchive = false;
        int result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), uvData);
        if (result == UVE_RNF) {
            isReleased = true;
            result = FileUtils.getRecord(readFiles.get("AOP.HIST"), uvData);
            if (result == UVE_RNF) {
                result = FileUtils.getRecord(readFiles.get("AOP.HIST.ARCHIVE"), uvData);
                isArchive = true;
                if (result == UVE_RNF) {
                    throw new NotFoundException(aoId + " not found");
                }
            }
        }
        return uvData;
    }

    private boolean getAoTracking(String aoTrackingId) throws RecordLockException {
        aoTrackingRec = new UvData();
        aoTrackingRec.setId(aoTrackingId);
        int result;
        result = FileUtils.lockRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
        if (result == LOCK_NO_LOCK) {
            throw new RecordLockException("AO.TRACKING lock error " + aoTrackingId);
        }
        result = FileUtils.getRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
        return (result != UVE_RNF);
    }

    private UvData getOrder(String orderId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result;
        if (isArchive) {
            result = FileUtils.getRecord(readFiles.get("ORDERS.HISTORY"), uvData);
            if (result == UVE_RNF) {
                throw new NotFoundException("Order# " + orderId + " not found");
            }
        } else if (isReleased) {
            result = FileUtils.getRecord(readFiles.get("ORDERS"), uvData);
            if (result == UVE_RNF) {
                result = FileUtils.getRecord(readFiles.get("ORDERS.HISTORY"), uvData);
                if (result == UVE_RNF) {
                    throw new NotFoundException("Order# " + orderId + " not found");
                }
            }
        } else {
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
        }
        return uvData;
    }

    private void setAoTracking() {
        int vals = aoTrackingRec.getData().dcount(2);
        if (vals == 0) {
            int result = FileUtils.deleteRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
            if (result == UVE_EIO) {
                progress.display("Couldn't delete AO.TRACKING " + aoTrackingRec.getId());
            }
        } else {
            int result = FileUtils.writeRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
            if (result == -1) {
                progress.display("Couldn't write AO.TRACKING " + aoTrackingRec.getId());
                FileUtils.unlockRecord(writeFiles.get("AO.TRACKING"), aoTrackingRec);
            }
        }
    }

    private void updateAoTracking(int val) {
        UniDynArray uda = aoTrackingRec.getData();
        for (int attr = 2; attr <= 8; attr++) {
            uda.delete(attr, val);
        }
        uda.delete(13, val);
        uda.delete(18, val);
        uda.replace(1, DynArray.sum(uda, 3));
        uda.replace(12, DynArray.sum(uda, 13));
        aoTrackingRec.setData(uda);
    }
}
