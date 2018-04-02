/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_NOERROR;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import asjava.uniobjects.UniObjectsTokens;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetAo197 extends BaseApp {

    boolean isHistory;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST of AFFILIATE.ORDERS ids is required");
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
            list3.clearList();
            list3 = getList(readSession, listName);
            while (!list3.isLastRecordRead()) {
                String aoId = list3.next().toString();
                if (aoId.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                Double pctDoneText = pctDone * 100;
                progress.display(aoId);
                progress.updateProgressBar(pctDone);
                
                UvData aoRec = new UvData();
                aoRec.setId(aoId);
                if (!findAo(aoRec)) {
                    progress.display("Can't find AO " + aoId);
                    continue;
                }
                boolean hasErrors = !aoRec.getData().extract(29).toString().isEmpty();
                String orderId = aoRec.getData().extract(30).toString();
                if (!orderId.isEmpty()) {
                    UvData orderRec = new UvData();
                    orderRec.setId(orderId);
                    if (getOrder(orderRec)) {
                        if (!setOrder(orderRec)) {
                            progress.display("Error writing ORDERS.RELEASE " + orderId);
                        }
                    }
                }
                findOrk(aoRec);
                if (!setAo(aoRec)) {
                    progress.display("Error locking AFFILIATE.ORDERS " + aoRec.getId());
                }
                if (hasErrors) {
                    UniDynArray errorList = aoRec.getData().extract(29);
                    int errorCount = errorList.dcount(1);
                    for (int val = 1; val <= errorCount; val++) {
                        String aeId = errorList.extract(1, val).toString();
                        UvData aeRec = new UvData();
                        aeRec.setId(aeId);
                        if (findAe(aeRec)) {
                            aeRec.getData().replace(197, aoRec.getData().extract(197).toString());
                            if (!setAe(aeRec)) {
                                progress.display("Error locking AFFILIATE.ERRORS " + aeRec.getId());
                            }
                        }
                    }
                }
            }
        } catch (UniSelectListException e) {

        } catch (UniSessionException ex) {
            Logger.getLogger(SetAo197.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    public boolean findAo(UvData record) {
        int result;
        isHistory = false;
        result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), record);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        return true;
    }

    public boolean findAe(UvData record) {
        int result;
        result = FileUtils.getRecord(readFiles.get("AFFILIATE.ERRORS"), record);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        return true;
    }

    private boolean setAe(UvData record) {
        UvData aeRec = new UvData();
        aeRec.setId(record.getId());
        int result = FileUtils.lockRecord(writeFiles.get("AFFILIATE.ERRORS"), aeRec);
        if (result == UniObjectsTokens.LOCK_NO_LOCK) {

            return false;
        }
        aeRec.setData(record.getData());
        result = FileUtils.writeRecord(writeFiles.get("AFFILIATE.ERRORS"), aeRec);
        return true;
    }

    private boolean setAo(UvData record) {
        UvData aoRec = new UvData();
        aoRec.setId(record.getId());
        int result = FileUtils.lockRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
        if (result == UniObjectsTokens.LOCK_NO_LOCK) {
            progress.display("Error locking AFFILIATE.ORDERS " + aoRec.getId());
            return false;
        }
        aoRec.setData(record.getData());
        result = FileUtils.writeRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
        return true;
    }

    public boolean findOrk(UvData record) {
        UniDynArray aoRec = record.getData();
        String aoId = record.getId();
        String[] idSegs = aoId.split("\\*");
        String divId = idSegs[1];
        String date = idSegs[3];
        String time = idSegs[4];
        String vendorId = aoRec.extract(181).toString();

        String baseOrkId = divId + "*" + date + "*" + time;
        UvData orkRec = new UvData();
        String batchStatus;
        for (int seq = 10; seq < 100; seq++) {
            String batchSeq = Integer.toString(seq);
            String orkId = baseOrkId + "*" + batchSeq;
            orkRec.setId(orkId);
            int result = FileUtils.getRecord(readFiles.get("ORDER.REL.KEYS"), orkRec);
            if (result == UVE_NOERROR || result == UVE_RNF) {
                batchStatus = orkRec.getData().extract(4).toString();
                if (batchStatus.equals("NP") || batchStatus.isEmpty()) {
                    aoRec.replace(197, orkId);
                    record.setData(aoRec);
                    return true;
                }
            }
        }
        record.getData().replace(197, baseOrkId + "*10");
        return false;
    }

    private boolean getOrder(UvData record) {
        UvData orderRec = new UvData();
        orderRec.setId(record.getId());
        int result = FileUtils.getRecord(writeFiles.get("ORDERS.RELEASE"), orderRec);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        record.setData(orderRec.getData());
        return true;
    }

    private boolean setOrder(UvData record) {
        UvData orderRec = new UvData();
        orderRec.setId(record.getId());
        int result = FileUtils.lockRecord(writeFiles.get("ORDERS.RELEASE"), orderRec);
        if (result == UniObjectsTokens.LOCK_NO_LOCK) {
            progress.display("Error locking ORDERS.RELEASE " + orderRec.getId());
            return false;
        }
        orderRec.setData(record.getData());
        result = FileUtils.writeRecord(writeFiles.get("ORDERS.RELEASE"), orderRec);
        return true;
    }
}
