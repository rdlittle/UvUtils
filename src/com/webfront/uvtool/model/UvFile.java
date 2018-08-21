/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.model;

/**
 *
 * @author rlittle
 */
public class UvFile {
    private String fileName;
    private boolean remote;
    private boolean local;
    private int appId;
    
    public UvFile(int aid, String name, boolean re, boolean lo) {
        appId = aid;
        fileName = name;
        remote = re;
        local = lo;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLocal() {
        return local ? 1 : 0;
    }
    
    public int getRemote() {
        return remote ? 1 : 0;
    }
    /**
     * @return the isRemote
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * @param isRemote the isRemote to set
     */
    public void setIsRemote(boolean isRemote) {
        this.remote = isRemote;
    }

    /**
     * @return the isLocal
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * @param isLocal the isLocal to set
     */
    public void setIsLocal(boolean isLocal) {
        this.local = isLocal;
    }

    /**
     * @return the appId
     */
    public int getAppId() {
        return appId;
    }

    /**
     * @param appId the appId to set
     */
    public void setAppId(int appId) {
        this.appId = appId;
    }
}
