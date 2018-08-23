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
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetAopVendorDivision extends BaseApp {

    public UvData aoTrackingRec;
    public UvData aoRec;
    public UvData gptsTransRec;
    public UvData orderRec;

    public HashMap<String, UvData> recordMap;

    boolean isHist;
    boolean isReleased;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST of AO.TRACKING ids is required");
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
                String aotId = list.next().toString();
                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                if (aotId.isEmpty()) {
                    continue;
                }
                init();
                try {
                    if(getAoTracking(aotId)) {
                       recordMap.put("PREV_AOT", aoTrackingRec);
                       int aoCount = aoTrackingRec.getData().dcount(2);
                    }
                } catch (RecordLockException ex) {
                    Logger.getLogger(SetAopVendorDivision.class.getName()).log(Level.SEVERE, null, ex);
                }
                recordMap.put("PREV_AOT", aoTrackingRec);
            }
        } catch (UniSelectListException e) {
            progress.display(e.getMessage());
            return false;
        } catch (UniSessionException ex) {
            progress.display(ex.getMessage());
            return false;
        }

        return true;
    }

    private void init() {
        recordMap = new HashMap<>();
        recordMap.put("PREV_AOT", null);
        recordMap.put("PREV_AO", null);
        recordMap.put("PREV_GPTS", null);
        recordMap.put("PREV_ORD", null);
        recordMap.put("NEW_AOT", null);
        recordMap.put("NEW_AO", null);
        recordMap.put("NEW_GPTS", null);
        recordMap.put("NEW_ORD", null);
    }

    private UvData getAo(String aoId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(aoId);
        isHist = false;
        uvData.setFileName("AFFILIATE.ORDERS");
        int result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), uvData);
        if (result == UVE_RNF) {
            uvData.setFileName("AOP.HIST");
            result = FileUtils.getRecord(readFiles.get("AOP.HIST"), uvData);
            if (result == UVE_RNF) {
                uvData.setFileName("AOP.HIST.ARCHIVE");
                result = FileUtils.getRecord(readFiles.get("AOP.HIST.ARCHIVE"), uvData);
                if (result == UVE_RNF) {
                    throw new NotFoundException(aoId + " not found");
                }
            }
        }
        isHist = true;
        return uvData;
    }

    private boolean getAoTracking(String aoTrackingId) throws RecordLockException {
        aoTrackingRec = new UvData();
        aoTrackingRec.setId(aoTrackingId);
        aoTrackingRec.setFileName("AO.TRACKING");
        int result;
        result = FileUtils.lockRecord(readFiles.get("AO.TRACKING"), aoTrackingRec);
        if (result == LOCK_NO_LOCK) {
            throw new RecordLockException("AO.TRACKING lock error " + aoTrackingId);
        }
        result = FileUtils.getRecord(readFiles.get("AO.TRACKING"), aoTrackingRec);
        return (result == UVE_RNF);
    }

    private UvData getGptsTrans(String gptsTransId) throws NotFoundException {
        gptsTransRec = new UvData();
        gptsTransRec.setId(gptsTransId);
        gptsTransRec.setFileName("GPTS.TRANS");
        return gptsTransRec;
    }

    private UvData getOrder(String orderId) throws NotFoundException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result;
        uvData.setFileName("ORDERS.RELEASE");
        result = FileUtils.getRecord(readFiles.get("ORDERS.RELEASE"), uvData);
        if (result == UVE_RNF) {
            uvData.setFileName("ORDER.REL.ERROR");
            result = FileUtils.getRecord(readFiles.get("ORDER.REL.ERROR"), uvData);
            if (result == UVE_RNF) {
                isReleased = true;
                uvData.setFileName("ORDERS");
                result = FileUtils.getRecord(readFiles.get("ORDERS"), uvData);
                if (result == UVE_RNF) {
                    uvData.setFileName("ORDERS.HISTORY");
                    result = FileUtils.getRecord(readFiles.get("ORDERS.HISTORY"), uvData);
                    if (result == UVE_RNF) {
                        throw new NotFoundException("Order# " + orderId + " not found");
                    }
                }
            }
        }
        return uvData;
    }
    
    private void releaseAll() {
        int result;
        for(String key : recordMap.keySet()) {
            UvData value = recordMap.get(key);
            if(value==null) {
                continue;
            }
            String fileName = value.getFileName();
            
        }
    }
}
