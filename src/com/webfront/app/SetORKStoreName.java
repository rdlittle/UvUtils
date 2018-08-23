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
public class SetORKStoreName extends BaseApp {

    UvData aggregatorRec;
    UvData ibvVendorRec;
    UvData storeRec;
    HashMap<String, String> names;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST of ORDER.REL.KEYS ids is required");
            teardown();
            return false;
        }
        names = new HashMap<>();
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
                String orkId = list3.next().toString();
                if (orkId.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                Double pctDoneText = pctDone * 100;
                progress.display(orkId);
                progress.updateProgressBar(pctDone);
                UvData orkRec = new UvData();
                orkRec.setId(orkId);
                int result = FileUtils.lockRecord(writeFiles.get("ORDER.REL.KEYS"), orkRec);
                switch (result) {
                    case -1:
                        progress.display("Error reading ORDER.REL.KEYS " + orkId);
                        continue;
                    case UVE_RNF:
                        progress.display("Can't find ORDER.REL.KEYS " + orkId);
                        FileUtils.unlockRecord(writeFiles.get("ORDER.REL.KEYS"), orkRec);
                        continue;
                }
                String storeName = getStoreName(orkRec);
                if (!storeName.isEmpty()) {
                    UniDynArray uda = orkRec.getData();
                    uda.replace(27, storeName);
                    orkRec.setData(uda);
                }
                result = FileUtils.writeRecord(writeFiles.get("ORDER.REL.KEYS"), orkRec);
                if (result == -1) {
                    progress.display("Error writing ORDER.REL.KEYS " + orkId);
                }
            }
            teardown();
        } catch (UniSessionException ex) {
            Logger.getLogger(SetORKStoreName.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(SetORKStoreName.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private String getStoreName(UvData orkRec) {
        String vendorId = orkRec.getData().extract(23).toString();
        String orkId = orkRec.getId();
        String[] idSegs = orkId.split("\\*");
        String divId = idSegs[0];
        String name;
        if (names.containsKey(divId)) {
            return names.get(divId);
        }
        if (isAggregator(vendorId)) {
            if (getStore(vendorId, divId)) {
                name = storeRec.getData().extract(1).toString().toUpperCase();
                names.put(divId, name);
                return name;
            }
        } else {
            if (isIbvVendor(vendorId)) {
                Result r = SysUtils.locate(divId, ibvVendorRec.getData(), 21);
                if (r.location > 0) {
                    name = ibvVendorRec.getData().extract(6, r.location).toString().toUpperCase();
                    names.put(divId, name);
                    return name;
                }
            }
        }
        return "";
    }

    private boolean isAggregator(String vendorId) {
        aggregatorRec = new UvData();
        aggregatorRec.setId(vendorId);
        int result = FileUtils.getRecord(readFiles.get("AFFILIATE.AGGREGATOR"), aggregatorRec);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        return true;
    }

    private boolean isIbvVendor(String vendorId) {
        ibvVendorRec = new UvData();
        ibvVendorRec.setId(vendorId);
        int result = FileUtils.getRecord(readFiles.get("IBV.VENDOR"), ibvVendorRec);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        return true;
    }

    private boolean getStore(String vendorId, String divId) {
        storeRec = new UvData();
        UvData xrefRec = new UvData();
        xrefRec.setId(divId);
        int result = FileUtils.getRecord(readFiles.get("AOP.DIV.XREF"), xrefRec);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        String storeId = vendorId + "*" + xrefRec.getData().extract(4).toString();
        storeRec.setId(storeId);
        result = FileUtils.getRecord(readFiles.get("AFFILIATE.STORE"), storeRec);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        return true;
    }
}
