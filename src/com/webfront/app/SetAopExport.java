/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniSessionException;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.util.UvConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class SetAopExport extends BaseApp {

    @Override
    public void setup(Program program, Profile readProfile, Profile writeProfile) {
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
            }
            openFiles(program.getFileList());

        } catch (UniSessionException ex) {
            Logger.getLogger(GetAffiliateErrors.class.getName()).log(Level.SEVERE, null, ex);
        }
        progress.updateLed(null, true);
    }

    @Override
    public boolean mainLoop() {
        
        return true;
    }

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

}
