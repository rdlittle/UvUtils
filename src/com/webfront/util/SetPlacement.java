package com.webfront.util;

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
import java.util.logging.Level;
import java.util.logging.Logger;

class SetPlacement {

    UniJava uv;
    UniFile localFile;
    UniSelectList list;
    static UniSession localSession;
    static UniSession remoteSession;
    static UniSubroutine remoteSub;
    UniCommand cmd;

    String remoteDomain;
    String remoteServer;
    String remoteUser;
    String remotePass;
    String remotePath;

    String localDomain;
    String localServer;
    String localUser;
    String localPass;
    String localPath;

    public SetPlacement() {
        try {
            uv = new UniJava();

            remoteSession = uv.openSession();
            remoteDomain = "maeagle.corp";
            remoteServer = "dmc";
            remoteUser = "bobl";
            remotePass = "rdl!!!1956";
            remotePath = "/uvfs/ma.accounts/dmc";

            localSession = uv.openSession();
            localServer = "localhost";
            localUser = "rlittle";
            localPass = "rll!1996";
            localPath = "/usr/local/madev";
        } catch (UniSessionException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public boolean connect() {
        try {
            System.out.println("Connecting to remote server...");
            remoteSession.connect(remoteServer + "." + remoteDomain, remoteUser, remotePass, remotePath);
            if (!remoteSession.isActive()) {
                return false;
            }
            localSession.connect(localServer, localUser, localPass, localPath);
            if (!localSession.isActive()) {
                return false;
            }
        } catch (UniSessionException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public UniSelectList doSelect() {
        try {
            cmd = localSession.command();
            //cmd.setCommand("SELECT AFFILIATE.ORDERS WITH 4 AND WITH 8 AND WITH 26 > \"0\" AND WITHOUT 151");
            cmd.setCommand("SELECT AFFILIATE.ORDERS WITHOUT 151 AND WITH 26 > \"0\"");
            cmd.exec();
            return localSession.selectList(0);
        } catch (UniSessionException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniCommandException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
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
                                System.out.println(Integer.toString(recordCount)+"  "+recordId);
                            }
                        }
                    } catch (UniFileException ex) {
                        Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (UniSelectListException ex) {
                        Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (UniSubroutineException ex) {
                        Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (UniSessionException ex) {
                Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
