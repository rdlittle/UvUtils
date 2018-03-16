/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetRebuildOrkFromAo {

    UniSession localSession;
    UniSession remoteSession;

    String remoteServer;
    String remoteUser;
    String remotePass;
    String remotePath;

    String localServer;
    String localUser;
    String localPass;
    String localPath;

    HashMap<String, UniFile> remoteFiles;
    HashMap<String, UniFile> localFiles;

    public ArrayList<String> fileNames;

    Double totalRecords;

    public SetRebuildOrkFromAo() {
        remoteServer = "dmc.maeagle.corp";
        remoteUser = "bobl";
        remotePass = "rdl!!!1956";
        remotePath = "/uvfs/ma.accounts/dmc";

        localServer = "localhost";
        localUser = "rlittle";
        localPass = "rll!1996";
        localPath = "/usr/local/madev";

        remoteFiles = new HashMap<>();
        localFiles = new HashMap();
        fileNames = new ArrayList<>();

        fileNames.add("AFFILIATE.ORDERS");
        fileNames.add("AOP.HIST");
        fileNames.add("AO.TRACKING");
        fileNames.add("ORDER.REL.KEYS");
        fileNames.add("ORDERS");
        fileNames.add("ORDERS.RELEASE");
        fileNames.add("ORDER.REL.ERROR");
        fileNames.add("IBV.VENDOR");
        fileNames.add("AFFILIATE.AGGREGATOR");
        fileNames.add("AFFILIATE.STORE");
    }

    public void openFiles() throws UniSessionException {
        for (String fileName : fileNames) {
            remoteFiles.put(fileName, remoteSession.openFile(fileName));
            localFiles.put(fileName, localSession.openFile(fileName));
        }
    }

    public UniSelectList getList(UniSession session, String listName) throws UniSessionException, UniSelectListException {
        UniSelectList list;
        list = session.selectList(0);
        list.getList(listName);
        UniDynArray recList = list.readList();
        totalRecords = new Double(recList.dcount());
        return list;
    }

    public void doUpdate(UniSelectList list) {
        
    }
    
    public static void main(String[] args) {
        SetRebuildOrkFromAo sro = new SetRebuildOrkFromAo();
        String listName = args[0];
        if (listName.isEmpty()) {
            System.out.println("Requires list name");
            return;
        }
        try {
            sro.localSession = UvConnection.newSession(sro.localUser, sro.localPass, sro.localServer, sro.localPath);
            sro.remoteSession = UvConnection.newSession(sro.remoteUser, sro.remotePass, sro.remoteServer, sro.remotePath);
            sro.openFiles();
            UniSelectList selList = sro.getList(sro.remoteSession, listName);
        } catch (UniSessionException ex) {
            Logger.getLogger(SetRebuildOrkFromAo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(SetRebuildOrkFromAo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
