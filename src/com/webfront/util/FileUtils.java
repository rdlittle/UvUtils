/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_NOERROR;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniObjectsTokens;
import static asjava.uniobjects.UniObjectsTokens.LOCK_MY_FILELOCK;
import com.webfront.u2.client.UvClient;
import com.webfront.u2.model.UvData;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class FileUtils {

    public static int deleteRecord(UniFile file, UvData data) {
        try {
            file.deleteRecord(data.getId());
        } catch (UniFileException ex) {
            return UniObjectsTokens.UVE_EIO;
        }
        return UniObjectsTokens.UVE_NOERROR;
    }
    
    public static int getRecord(UniFile file, UvData data) {
        UniDynArray record;
        try {
            if (data.getValue() > 0) {
                record = new UniDynArray(file.readField(data.getId(), data.getField()));
            } else {
                if (data.getField() > 0) {
                    record = new UniDynArray(file.readField(data.getId(), data.getField()));
                } else {
                    record = new UniDynArray(file.read(data.getId()));
                }
            }
        } catch (UniFileException ex) {
            data.setData(new UniDynArray());
            if (ex.getErrorCode() == UniObjectsTokens.UVE_RNF) {
                return UniObjectsTokens.UVE_RNF;
            }
            return -1;
        }
        data.setData(record);
        return UniObjectsTokens.UVE_NOERROR;
    }

    public static int lockRecord(UniFile file, UvData data) {
        try {
            file.lockRecord(data.getId(), UniObjectsTokens.UVT_EXCLUSIVE_READ);
            if (!file.isRecordLocked()) {
                return UniObjectsTokens.LOCK_NO_LOCK;
            }
        } catch (UniFileException ufe) {
            if (ufe.getErrorCode() == UniObjectsTokens.UVE_RNF) {
                return UniObjectsTokens.UVE_RNF;
            }
            Logger.getLogger(UvClient.class.getName()).log(Level.SEVERE, null, ufe);
            return UniObjectsTokens.LOCK_NO_LOCK;
        }
        if(getRecord(file,data)!= UVE_NOERROR) {
            return UniObjectsTokens.LOCK_NO_LOCK;
        }
        return LOCK_MY_FILELOCK;
    }

    public static int writeRecord(UniFile file, UvData data) {
        try {
            file.setReleaseStrategy(UniObjectsTokens.WRITE_RELEASE);
            if (data.getValue() > 0) {
                file.writeField(data.getId(), data.getData(), data.getField(), data.getValue());
            } else {
                if (data.getField() > 0) {
                    file.writeField(data.getId(), data.getData(), data.getField());
                } else {
                    file.write(data.getId(), data.getData());
                }
            }
        } catch (UniFileException ex) {
            try {
                if (file.isRecordLocked()) {
                    file.unlockRecord(data.getId());
                }
            } catch (UniFileException ex1) {
                Logger.getLogger(UvClient.class.getName()).log(Level.SEVERE, null, ex1);
                return -1;
            }
            Logger.getLogger(UvClient.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        return 0;
    }
    
    public static int unlockRecord(UniFile file, UvData data) {
        try {
            file.unlockRecord(data.getId());
        } catch (UniFileException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        return UniObjectsTokens.UVE_NOERROR;
    }
}
