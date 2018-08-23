/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniString;
import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniObjectsTokens;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import com.webfront.util.UvConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class GetCommissionErrors extends BaseApp {

    @Override
    public void teardown() {
        try {
            closeFiles();
            readSession.disconnect();
            writeSession.disconnect();
            progress.updateLed(null, false);
        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniFileException ex) {
            Logger.getLogger(GetCommissionErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean mainLoop() {
        String id;
        int result;
        progress.updateProgressBar(0D);
        try {
            UvData paramsRec = new UvData();
            paramsRec.setId("BAD.AO.COMM");
            result = FileUtils.getRecord(readFiles.get("PARAMS"), paramsRec);
            if (result != UniObjectsTokens.UVE_NOERROR) {
                System.out.println("PARAMS BAD.AO.COMM not found");
                return false;
            }
            ArrayList<String> badCommIds = new ArrayList<>();
            int idCount = paramsRec.getData().dcount();
            for (int attr = 1; attr <= idCount; attr++) {
                badCommIds.add(paramsRec.getData().extract(attr).toString());
            }
            UniSelectList list = readSession.selectList(3);
            list.getList(listName);
            UniDynArray temp = list.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            list = readSession.selectList(0);
            list.getList(listName);
            UniString newList = new UniString();
            ArrayList<String> badOrderIds = new ArrayList<>();
            while (!list.isLastRecordRead()) {
                id = list.next().toString();
                if (id.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                progress.display(itemsDone.intValue()+ " of " + itemCount.intValue() + " : " + (pctDone.intValue() * 1000) + "%");
                UvData ordDetailRec = new UvData();
                ordDetailRec.setId(id);
                UvData orderRec = new UvData();
                orderRec.setId(id);
                result = FileUtils.getRecord(readFiles.get("ORDERS.DETAIL"), ordDetailRec);
                if (result != UniObjectsTokens.UVE_NOERROR) {
                    progress.display("ORDERS.DETAIL " + id + " not found");
                    continue;
                }
                result = FileUtils.getRecord(readFiles.get("ORDERS.RELEASE"), orderRec);
                if (result != UniObjectsTokens.UVE_NOERROR) {
                    progress.display("ORDERS.RELEASE " + id + " not found");
                    continue;
                }
                String vendorId = orderRec.getData().extract(181, 1).toString();
                String storeId = orderRec.getData().extract(157, 1).toString();
                String targetId = vendorId.concat("*").concat(storeId);
                if (!badCommIds.contains(targetId)) {
                    continue;
                }
                int vals = ordDetailRec.getData().dcount(6);
                for (int val = 1; val <= vals; val++) {
                    String ibvVal = ordDetailRec.getData().extract(6, val).toString();
                    String commVal = ordDetailRec.getData().extract(39, val).toString();
                    if (ibvVal.length() == 3 || commVal.length() == 3) {
                        badOrderIds.add(id);
                        break;
                    }
                }
            }
            Iterator<String> iterator = badOrderIds.iterator();
            while (iterator.hasNext()) {
                String s = iterator.next();
                newList.append(s + UniObjectsTokens.AT_FM);
            }
            list.clearList();
            list.formList(newList);
            list.saveList("RDL.BAD.OD2");
        } catch (UniSessionException ex) {

        } catch (UniSelectListException ex) {

        }
        return true;
    }

    @Override
    public void setup(Program pgm, Profile readProfile, Profile writeProfile) {
        if (readProfile == null) {
            try {
                throw new Exception("You must specify read profile");
            } catch (Exception ex) {
                Logger.getLogger(GetCommissionErrors.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(listName.isEmpty()) {
            try {
                throw new Exception("List name is required");
            } catch (Exception ex) {
                Logger.getLogger(GetCommissionErrors.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            readSession = UvConnection.newSession(readProfile);
            readSession.connect();
            if(writeProfile != null) {
                writeSession = UvConnection.newSession(writeProfile);
                writeSession.connect();
            }
            openFiles(pgm.getFileList());
            
        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
        progress.updateLed(null, true);
        listName = pgm.getListName();
    }

}
