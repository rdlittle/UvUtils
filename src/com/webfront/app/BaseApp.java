/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniobjects.UniCommand;
import asjava.uniobjects.UniCommandException;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.u2.model.UvFile;
import com.webfront.u2.util.Progress;
import com.webfront.util.UvConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class BaseApp extends AbstractApp {

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

    Double totalRecords;
    String now;
    String listName;

    Progress progress;
    Program program;

    public void openFiles(ArrayList<UvFile> fileList) throws UniSessionException {
        for (UvFile uvf : fileList) {
            if (uvf.isRead()) {
                readFiles.put(uvf.getFileName(), readSession.openFile(uvf.getFileName()));
            } else {
                String fname = uvf.getFileName();
                UniFile uf = writeSession.openFile(fname);
                writeFiles.put(uvf.getFileName(), writeSession.openFile(uvf.getFileName()));
            }
        }
    }

    public void closeFiles() throws UniFileException {
        for (UniFile uf : readFiles.values()) {
            uf.close();
        }
        for (UniFile uf : writeFiles.values()) {
            uf.close();
        }
    }

    public UniSelectList getList(UniSession session, String listName) throws UniSessionException, UniSelectListException {
        UniSelectList list;
        list = session.selectList(0);
        list.getList(listName);
        return list;
    }
    
    public UniSelectList doSelect(UniSession session, String[] criteria) throws UniSessionException, UniCommandException {
        UniCommand command;
        for (String cmd : criteria) {
            command = session.command();
            command.setCommand(cmd);
            command.exec();
        }
        UniSelectList list = session.selectList(0);
        return list;
    }

    public String[] getDateTime() {
        String dt[] = new String[2];
        try {
            UniSubroutine dtSub = readSession.subroutine("getUtilTimeDateStamp.uvs", 3);
            UniDynArray iList = new UniDynArray();
            UniDynArray oList = new UniDynArray();
            UniDynArray eList = new UniDynArray();
            dtSub.setArg(0, iList);
            dtSub.setArg(1, oList);
            dtSub.setArg(2, eList);
            dtSub.call();
            oList = new UniDynArray(dtSub.getArg(1));
            dt[0] = oList.extract(6).toString();
            String t = oList.extract(2).toString();
            dt[1] = t.split("\\.")[0];
            return dt;
        } catch (UniSessionException ex) {
            Logger.getLogger(BaseApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSubroutineException ex) {
            Logger.getLogger(BaseApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dt;
    }

    public void setProgress(Progress p) {
        this.progress = p;
    }

    @Override
    public void teardown() {
        try {
            closeFiles();
            if (readSession.isActive()) {
                readSession.disconnect();
            }
            if (writeSession.isActive()) {
                writeSession.disconnect();
            }
            progress.updateLed(null, false);
            progress.updateProgressBar(0D);
        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniFileException ex) {
            Logger.getLogger(GetCommissionErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean mainLoop() {
        return true;
    }

    @Override
    public void setup(Program program, Profile readProfile, Profile writeProfile) {
        this.program = program;
        if (readProfile == null) {
            try {
                throw new Exception("You must specify read profile");
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
            } else {
                writeSession = readSession;
            }
            openFiles(program.getFileList());

        } catch (UniSessionException ex) {
            Logger.getLogger(BaseApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        listName = program.getListName();
        progress.updateLed(null, true);
    }

}
