/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.u2.model.UvData;
import com.webfront.u2.model.UvFile;
import com.webfront.u2.util.Progress;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author rlittle
 */
public abstract class AbstractApp {
    
    UniSession readSession;
    UniSession writeSession;

    String remoteServer;
    String remoteUser;
    String remotePass;
    String remotePath;

    String localServer;
    String localUser;
    String localPass;
    String localPath;

    final HashMap<String, UniFile> readFiles = new HashMap<>();
    final HashMap<String, UniFile> writeFiles = new HashMap<>();

    public ArrayList<String> fileNames;

    Profile localProfile;
    Profile remoteProfile;

    UvData aoRec;
    UvData aeRec;

    Double totalRecords;
    String now;
    String listName;
    
    Progress progress;
    
    public void openFiles(ArrayList<UvFile> fileList) throws UniSessionException {
        for(UvFile uvf : fileList) {
            if(uvf.isRead()) {
                readFiles.put(uvf.getFileName(), readSession.openFile(uvf.getFileName()));
            } else {
                String fname = uvf.getFileName();
                UniFile uf = writeSession.openFile(fname);
                writeFiles.put(uvf.getFileName(), writeSession.openFile(uvf.getFileName()));
            }
        }
    }
    
    public void closeFiles() throws UniFileException {
        for(UniFile uf : readFiles.values()) {
            uf.close();
        }
        for(UniFile uf : writeFiles.values()) {
            uf.close();
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
    
    public void setProgress(Progress p) {
        this.progress = p;
    }
    
    public abstract void teardown();

    public abstract boolean mainLoop();

    public abstract void setup(Program p, Profile readProfle, Profile writeProfile);

}
