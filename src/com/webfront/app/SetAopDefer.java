/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.u2.model.Prompt;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetAopDefer extends BaseApp {

    String aoId;
    String orkId;
    UniSubroutine subroutine;
    int svrStatus;
    String svrCtrlCode;
    String svrMessage;

    Double itemCount;
    Double itemsDone;
    Double pctDone;

    UniSelectList list;

    @Override
    public boolean mainLoop() {
        for (Prompt prp : program.getPromptList()) {
            String pMsg = prp.getMessage();
            if (pMsg.equalsIgnoreCase("AFFILIATE.ORDERS id")) {
                aoId = prp.getResponse();
            }
            if (pMsg.equalsIgnoreCase("ORDER.REL.KEYS id")) {
                orkId = prp.getResponse();
            }
        }

        if (aoId == null && listName.isEmpty()) {
            progress.display("AFFILIATE.ORDERS id or list name is required");
            teardown();
            return false;
        }
        if (orkId == null || orkId.isEmpty()) {
            progress.display("ORDER.REL.KEYS id is required");
            teardown();
            return false;
        }

        try {
            subroutine = writeSession.subroutine("AOP.DEFER", 3);
        } catch (UniSessionException ex) {
            progress.display(ex.getMessage());
            teardown();
            return false;
        }

        if (listName.isEmpty()) {
            try {
                list = readSession.selectList(0);
                itemCount = 1D;
                itemsDone = 0D;
                pctDone = 0D;
                UniDynArray uda = new UniDynArray();
                uda.replace(1, aoId);
                list.formList(uda);
            } catch (UniSessionException ex) {
                Logger.getLogger(SetAopDefer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UniSelectListException ex) {
                Logger.getLogger(SetAopDefer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                UniSelectList list3 = readSession.selectList(3);
                list3.getList(listName);
                UniDynArray temp = list3.readList();
                itemCount = new Double(temp.dcount());
                itemsDone = new Double(0);
                pctDone = new Double(0);
                list = getList(readSession, listName);
            } catch (UniSessionException ex) {
                Logger.getLogger(SetAopDefer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UniSelectListException ex) {
                Logger.getLogger(SetAopDefer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        while (!list.isLastRecordRead()) {
            try {
                aoId = list.next().toString();
                if(aoId.isEmpty()) {
                    continue;
                }
                try {
                    if (!callSub(aoId, orkId)) {
                        progress.display(svrMessage);
                    } else {
                        progress.display(aoId+" deferred");
                    }
                } catch (UniSessionException ex) {
                    progress.display(ex.getMessage());
                    teardown();
                    return false;
                } catch (UniSubroutineException ex) {
                    progress.display(ex.getMessage());
                    teardown();
                    return false;
                }
            } catch (UniSelectListException ex) {
                Logger.getLogger(SetAopDefer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        teardown();
        return true;
    }

    private boolean callSub(String affiliateOrderId, String orderRelKeysId) throws UniSessionException, UniSubroutineException {
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();
        iList.insert(1, affiliateOrderId);
        iList.insert(2, orderRelKeysId);
        subroutine.setArg(0, iList);
        subroutine.setArg(1, oList);
        subroutine.setArg(2, eList);
        subroutine.call();
        eList = subroutine.getArgDynArray(2);
        svrStatus = Integer.parseInt(eList.extract(1).toString());
        svrCtrlCode = eList.extract(5).toString();
        svrMessage = eList.extract(2).toString();
        return (svrStatus == 0);
    }

}
