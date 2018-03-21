/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.*;
import asjava.uniobjects.UniObjectsTokens;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import com.webfront.util.Result;
import com.webfront.util.SysUtils;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetRebuildOrkFromAo extends BaseApp {

    HashMap<String, String> storeNameList;

    public boolean isHistory;
    public boolean hasOrder;

    UvData aoRec;
    UvData orderRec;
    UvData storeRec;
    
    String now;

    public SetRebuildOrkFromAo() {
        storeNameList = new HashMap();
    }

    @Override
    public void teardown() {
        super.teardown();
    }

    @Override
    public boolean mainLoop() {
        String id;
        try {
            UniSubroutine sub = readSession.subroutine("getUtilTimeDateStamp.uvs", 3);
            sub.setArg(0, new UniDynArray());
            sub.setArg(1, new UniDynArray());
            sub.setArg(2, new UniDynArray());
            sub.call();
            UniDynArray oList = new UniDynArray(sub.getArg(1));
            UniDynArray eList = new UniDynArray(sub.getArg(2));
            int svrStatus = Integer.parseInt(eList.extract(1).toString());
            if (svrStatus == -1) {
                progress.display(eList.extract(2).toString());
                readSession.disconnect();
                return false;
            }
            String date = oList.extract(6).toString();
            String fullTime = oList.extract(2).toString();
            String[] time = fullTime.split("\\.");
            now = date + "*" + time[0];
            
            UniSelectList list = readSession.selectList(3);
            list.getList(listName);
            UniDynArray temp = list.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            list = readSession.selectList(0);
            list.getList(listName);
            while (!list.isLastRecordRead()) {
                id = list.next().toString();
                if (id.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                Double pctDoneText = pctDone * 100;
                progress.display(itemsDone.intValue() + " of " + itemCount.intValue() + " : " + (pctDoneText.intValue()) + "%");
                progress.updateProgressBar(pctDone);
                aoRec = new UvData();
                aoRec.setId(id);
                if (!findAo(aoRec)) {
                    progress.display("Can't file affiliate order " + id);
                    continue;
                }
                String orkId = aoRec.getData().extract(197).toString();
                String orderId = aoRec.getData().extract(30).toString();
                String aggId = aoRec.getData().extract(181, 1).toString();
                String storeId = aoRec.getData().extract(157, 1).toString();
                String affiliateStoreId = aggId + "*" + storeId;
                if (orkId.isEmpty()) {
                    progress.display("No ORDER.REL.KEY in AO " + id);
                    continue;
                }
                hasOrder = !orderId.isEmpty();
                if (!orderId.isEmpty()) {
                    if (!findOrder(orderId)) {
                        progress.display("Can't read order " + orderId);
                        continue;
                    }
                }
                if (!getStore(affiliateStoreId)) {
                    if (!getIbvVendor(aggId, storeId)) {
                        progress.display("Can't file affiliate store " + affiliateStoreId);
                        continue;
                    }
                }
                updateOrderRelKeys(orkId);
            }
        } catch (UniSelectListException ex) {
            Logger.getLogger(SetRebuildOrkFromAo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSessionException ex) {
            Logger.getLogger(SetRebuildOrkFromAo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSubroutineException ex) {
            Logger.getLogger(SetRebuildOrkFromAo.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    @Override
    public void setup(Program program, Profile readProfile, Profile writeProfile) {
        super.setup(program, readProfile, writeProfile);
    }

    public void openFiles(UniSession session) throws UniSessionException {
        for (String fileName : fileNames) {
            readFiles.put(fileName, session.openFile(fileName));
        }
    }

//    public UniSelectList getList(UniSession session, String listName) throws UniSessionException, UniSelectListException {
//        UniSelectList list;
//        list = session.selectList(0);
//        list.getList(listName);
//        UniDynArray recList = list.readList();
//        totalRecords = new Double(recList.dcount());
//        return list;
//    }

    public boolean findAo(UvData record) {
        isHistory = false;
        int result;
        result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), record);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                result = FileUtils.getRecord(readFiles.get("AOP.HIST"), record);
                switch (result) {
                    case -1:
                        return false;
                    case UVE_NOERROR:
                        isHistory = true;
                        break;
                    default:
                        return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    public boolean findOrder(String orderId) {
        orderRec = new UvData();
        orderRec.setId(orderId);
        int result;
        if (isHistory) {
            result = FileUtils.getRecord(readFiles.get("ORDERS"), orderRec);
        } else {
            result = FileUtils.getRecord(readFiles.get("ORDERS.RELEASE"), orderRec);
        }
        if (result == UniObjectsTokens.UVE_RNF) {
            result = FileUtils.getRecord(readFiles.get("ORDER.REL.ERROR"), orderRec);
        }
        if (result != UniObjectsTokens.UVE_NOERROR) {
            return false;
        }
        return true;
    }

    public boolean getStore(String id) {
        storeRec = new UvData();
        storeRec.setId(id);
        if (storeNameList.containsKey(id)) {
            UniDynArray uda = new UniDynArray();
            uda.replace(1, storeNameList.get(id));
            storeRec.setData(uda);
            return true;
        }
        int result = FileUtils.getRecord(readFiles.get("AFFILIATE.STORE"), storeRec);
        switch (result) {
            case -1:
                return false;
            case UniObjectsTokens.UVE_RNF:
                return false;
        }
        storeNameList.put(id, storeRec.getData().extract(1).toString().toUpperCase());
        return true;
    }

    public boolean getIbvVendor(String v, String s) {
        String storeKey = v + "*" + s;
        if (storeNameList.containsKey(storeKey)) {
            storeRec = new UvData();
            UniDynArray uda = new UniDynArray();
            uda.replace(1, storeNameList.get(storeKey));
            storeRec.setData(uda);
            return true;
        }
        UvData ibvVendorRec = new UvData();
        ibvVendorRec.setId(v);
        int result = FileUtils.getRecord(readFiles.get("IBV.VENDOR"), ibvVendorRec);
        switch (result) {
            case -1:
                return false;
            case UniObjectsTokens.UVE_RNF:
                return false;
        }
        UniDynArray uda = ibvVendorRec.getData();
        int vals = uda.dcount(5);
        for (int val = 1; val <= vals; val++) {
            String sid = uda.extract(5, val).toString();
            if (sid.equals(s)) {
                storeRec = new UvData();
                String storeName = uda.extract(6, val).toString();
                uda = new UniDynArray();
                uda.replace(1, storeName.toUpperCase());
                storeRec.setData(uda);
                storeNameList.put(storeKey, storeName);
                return true;
            }
        }
        return false;
    }

    public boolean updateOrderRelKeys(String id) {
        UvData orkRec = new UvData();
        orkRec.setId(id);
        int result = FileUtils.getRecord(readFiles.get("ORDER.REL.KEYS"), orkRec);
        UniDynArray ork = orkRec.getData();
        String[] batchSegments = orkRec.getId().split("\\*");
        int batchSeq = Integer.parseInt(batchSegments[3]);
        String batchStatus;
        if (batchSeq >= 300) {
            batchStatus = "PE";
        } else if (batchSeq >= 100) {
            batchStatus = "RP";
        } else if (batchSeq == 3) {
            batchStatus = "NA";
        } else if (isHistory) {
            batchStatus = "P";
        } else {
            batchStatus = "NP";
        }

        if (result == UniObjectsTokens.UVE_RNF) {
            ork.replace(1, "IBV");
            ork.replace(2, "0");
            ork.replace(3, "0");
            ork.replace(4, batchStatus);
            ork.replace(12, "0");
            ork.replace(13, "0");
            ork.replace(14, "0");
            ork.replace(15, "0");
            ork.replace(16, "0");
            ork.replace(21, "0");
            ork.replace(22, "0");
            ork.replace(23, aoRec.getData().extract(181, 1).toString());
            ork.replace(27, storeRec.getData().extract(1, 1).toString());
            ork.replace(30, now);
        } else {
            String orkDateTime = ork.extract(30).toString();
            if (!orkDateTime.equals(now)) {
                ork.replace(1, "IBV");
                ork.replace(2, "0");
                ork.replace(3, "0");
                ork.replace(4, batchStatus);
                ork.replace(12, "0");
                ork.replace(13, "0");
                ork.replace(14, "0");
                ork.replace(15, "0");
                ork.replace(16, "0");
                ork.replace(21, "0");
                ork.replace(22, "0");
                ork.replace(30, now);
            }
        }

        boolean hasErrors = !aoRec.getData().extract(29, 1).toString().isEmpty();
        boolean hasApproval = ork.extract(6).toString().equals("Y");
        int aoReportedComm = SysUtils.sum(aoRec.getData().extract(173));
        int ordCalcComm = 0;
        if (hasOrder) {
            ordCalcComm = SysUtils.sum(orderRec.getData().extract(176));
        } else {
            ordCalcComm = SysUtils.sum(aoRec.getData().extract(176));
        }
        int aoIbv = SysUtils.sum(aoRec.getData().extract(149));

        int orderCount = Integer.parseInt(ork.extract(2).toString());
        int calcComm = Integer.parseInt(ork.extract(3).toString());
        int reportedComm = Integer.parseInt(ork.extract(12).toString());
        int ibv = Integer.parseInt(ork.extract(13).toString());
        int errorCount = Integer.parseInt(ork.extract(22).toString());
        int reportedCommErrors = Integer.parseInt(ork.extract(14).toString());
        int ibvErrors = Integer.parseInt(ork.extract(15).toString());
        int calcCommErrors = Integer.parseInt(ork.extract(16).toString());
        int approvedAmt = Integer.parseInt(ork.extract(21).toString());

        if (hasErrors) {
            errorCount += 1;
            reportedCommErrors += aoReportedComm;
            calcCommErrors += ordCalcComm;
            ibvErrors += aoIbv;
        } else {
            orderCount += 1;
            calcComm += ordCalcComm;
            reportedComm += aoReportedComm;
            ibv += aoIbv;
        }

        if (hasApproval) {
            approvedAmt += aoReportedComm;
        }

        ork.replace(2, orderCount);
        ork.replace(3, calcComm);
        ork.replace(12, reportedComm);
        ork.replace(13, ibv);
        ork.replace(14, reportedCommErrors);
        ork.replace(15, ibvErrors);
        ork.replace(16, calcCommErrors);
        ork.replace(21, approvedAmt);
        ork.replace(22, errorCount);

        String orderId = aoRec.getData().extract(30).toString();
        if (!orderId.isEmpty()) {
            Result r = SysUtils.locate(orderId, ork, 18);
            if (!r.isSuccess) {
                if (r.location == -1) {
                    ork.replace(18, orderId);
                } else {
                    ork.insert(18, r.location, orderId);
                }
            }
        }

        orkRec.setData(ork);
        result = FileUtils.lockRecord(readFiles.get("ORDER.REL.KEYS"), orkRec);
        if (result == UniObjectsTokens.LOCK_NO_LOCK) {
            System.out.println("Error locking ORDER.REL.KEYS " + orkRec.getId());
        }
        result = FileUtils.writeRecord(readFiles.get("ORDER.REL.KEYS"), orkRec);
        if (result == -1) {
            result = FileUtils.unlockRecord(readFiles.get("ORDER.REL.KEYS"), orkRec);
            System.out.println("Error writing ORDER.REL.KEYS " + orkRec.getId());
            return false;
        }
        return true;
    }

}
