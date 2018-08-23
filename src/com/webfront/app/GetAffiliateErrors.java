/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniString;
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
public class GetAffiliateErrors extends BaseApp {

    UvData aoRec;
    UvData aeRec;
    
    @Override
    public void setup(Program program, Profile readProfile, Profile writeProfile) {
        try {
            readSession = UvConnection.newSession(readProfile);
            readSession.connect();
            readSession = UvConnection.newSession(writeProfile);
            readSession.connect();
            openFiles(program.getFileList());
        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void teardown() {
        try {
//            remoteSession.disconnect();
            readSession.disconnect();
        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean mainLoop() {
        String id;
        int result;
        try {
            UniSelectList list = readSession.selectList(3);
            list.getList(listName);
            UniDynArray temp = list.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            list = readSession.selectList(0);
            list.getList(listName);
            UniString newList = new UniString();
            ArrayList<String> badIds = new ArrayList<>();
            while (!list.isLastRecordRead()) {
                id = list.next().toString();
                if (id.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = (itemsDone / itemCount) * 100;
                System.out.println(itemsDone.intValue() + " of " + itemCount.intValue() + " : " + pctDone.intValue() + "%");
                aoRec = new UvData();
                aoRec.setId(id);
                result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS.MA"), aoRec);
                if (result != UniObjectsTokens.UVE_NOERROR) {
                    continue;
                }
                int vals = aoRec.getData().dcount(29);
                for(int val=1; val<=vals; val++) {
                    String aeId = aoRec.getData().extract(29,val).toString();
                    aeRec = new UvData();
                    aeRec.setId(aeId);
                    result = FileUtils.getRecord(readFiles.get("AFFILIATE.ERRORS.MA"), aeRec);
                    if(result == UniObjectsTokens.UVE_RNF) {
                        badIds.add(id);
                        break;
                    }
                }
            }
            Iterator<String> iterator = badIds.iterator();
            while(iterator.hasNext()) {
                String s = iterator.next();
                newList.append(s+UniObjectsTokens.AT_FM);
            }
            list.clearList();
            list.formList(newList);
            list.saveList("RDL.BAD.AO");
        } catch (UniSessionException ex) {

        } catch (UniSelectListException ex) {

        }
        return true;
    }

}
