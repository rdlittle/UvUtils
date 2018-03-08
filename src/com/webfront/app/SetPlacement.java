package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniobjects.UniCommand;
import asjava.uniobjects.UniCommandException;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniJava;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.util.UvConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

class SetPlacement extends BaseApp {

    UniJava uv;
    UniFile localFile;
    UniSelectList list;
    static UniSession localSession;
    static UniSession remoteSession;
    static UniSubroutine remoteSub;
    UniCommand cmd;

    public SetPlacement() {
    }

    @Override
    public boolean mainLoop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setup(Program program, Profile readProfile, Profile writeProfile) {
        if (readProfile == null) {
            try {
                throw new Exception("You must specify read profile");
            } catch (Exception ex) {
                Logger.getLogger(GetCommissionErrors.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (listName.isEmpty()) {
            try {
                throw new Exception("List name is required");
            } catch (Exception ex) {
                Logger.getLogger(GetCommissionErrors.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            readSession = UvConnection.newSession(readProfile);
            readSession.connect();
            if (writeProfile != null) {
                writeSession = UvConnection.newSession(writeProfile);
                writeSession.connect();
            }
            openFiles(program.getFileList());

        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
        progress.updateLed(null, true);
        listName = program.getListName();
    }

    @Override
    public void teardown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean connect() {

        return true;
    }

    public UniSelectList doSelect() {
        try {
            cmd = readSession.command();
            //cmd.setCommand("SELECT AFFILIATE.ORDERS WITH 4 AND WITH 8 AND WITH 26 > \"0\" AND WITHOUT 151");
            cmd.setCommand("SELECT AFFILIATE.ORDERS WITHOUT 151 AND WITH 26 > \"0\"");
            cmd.exec();
            return readSession.selectList(0);

        } catch (UniSessionException ex) {
            Logger.getLogger(SetPlacement.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (UniCommandException ex) {
            Logger.getLogger(SetPlacement.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String callSub(String payingId, String orderDate) throws UniSubroutineException {
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();

        iList.insert(1, payingId);
        iList.insert(3, "U");
        iList.insert(4, "ENG");
        iList.insert(5, orderDate);
        iList.insert(6, "IBV");

        remoteSub.setArg(0, iList);
        remoteSub.setArg(1, oList);
        remoteSub.setArg(2, eList);
        remoteSub.call();

        eList = new UniDynArray(remoteSub.getArg(2));
        int svrStatus = Integer.parseInt(eList.extract(1).toString());
        String svrCtrlCode = eList.extract(5).toString();
        String svrMessage = eList.extract(2).toString();
        if (svrStatus == -1) {
            System.out.println("Error " + svrCtrlCode + " " + svrMessage);
            return "";
        }
        oList = new UniDynArray(remoteSub.getArg(1));
        return oList.extract(1).toString();
    }

    public static void main(String[] args) {
        SetPlacement sp = new SetPlacement();
        if (!sp.connect()) {
            System.out.println("Can't connect");
        } else {
            try {
                sp.remoteSub = remoteSession.subroutine("SET.BV.AUTO.PLACEMENT.OP", 3);
                sp.localFile = localSession.openFile("AFFILIATE.ORDERS");
                sp.list = sp.doSelect();
                int recordCount = 0;
                while (!sp.list.isLastRecordRead()) {
                    try {
                        String recordId = sp.list.next().toString();
                        if (recordId != null && !recordId.isEmpty()) {
                            UniDynArray aoRec = new UniDynArray(sp.localFile.read(recordId));
                            String payingId = aoRec.extract(8).toString();
                            String orderDate = aoRec.extract(161).toString();
                            String placement = sp.callSub(payingId, orderDate);
                            if (!placement.isEmpty()) {
                                recordCount++;
                                aoRec.replace(151, placement);
                                sp.localFile.write(recordId, aoRec);
                                System.out.println(Integer.toString(recordCount) + "  " + recordId);

                            }
                        }
                    } catch (UniFileException ex) {
                        Logger.getLogger(SetPlacement.class
                                .getName()).log(Level.SEVERE, null, ex);

                    } catch (UniSelectListException ex) {
                        Logger.getLogger(SetPlacement.class
                                .getName()).log(Level.SEVERE, null, ex);

                    } catch (UniSubroutineException ex) {
                        Logger.getLogger(SetPlacement.class
                                .getName()).log(Level.SEVERE, null, ex);

                    }
                }
            } catch (UniSessionException ex) {
                Logger.getLogger(SetPlacement.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
