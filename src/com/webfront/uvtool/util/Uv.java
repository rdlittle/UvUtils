/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.util;

import asjava.uniobjects.UniJava;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import com.webfront.uvtool.model.Profile;

/**
 *
 * @author rlittle
 */
public class Uv {

    private static UniJava uj;
    private UniSession session;
    private String hostName;
    private String accountPath;
    private String userName;
    private String password;
    
    private Profile profile;
    
    private static Uv uv;

    public static enum SelectType {
        LIST, QUERY;
    }

    public static enum Missing {
        CREATE, IGNORE;
    }

    public static enum Existing {
        OVERWRITE, PRESERVE;
    }
    
    public static enum SelectFrom {
        SOURCE, DESTINATION;
    }

    protected Uv() {
        if(Uv.uj == null) {
            Uv.uj = new UniJava();
        }
    }

    protected Uv(String h, String u, String p, String a) {
        this();
        this.hostName = h;
        this.userName = u;
        this.password = p;
        this.accountPath = a;
    }
    
    public static Uv newInstance(String h, String u, String p, String a) {
        return new Uv(h,u,p,a);
    }

    public static Uv newInstance(Profile p) {
        Uv u = new Uv();
        u.setProfile(p);
        return u;
    }

    public boolean connect() throws UniSessionException {
        session = new UniSession();
        session.connect(this.hostName, this.userName, this.password, this.accountPath);
        return session.isActive();
    }

//    public UniSession newSession(String h, String u, String pw, String p) throws UniSessionException {
//        UniSession usession = new UniSession();
//            usession.connect(this.hostName, this.userName, this.password, this.accountPath);
//            if(!usession.isActive()) {
//                return null;
//            }
//        return usession;
//    }

    public void disconnect() throws UniSessionException {
        if (session.isActive()) {
            session.disconnect();
        }
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * @return the accountName
     */
    public String getAccountPath() {
        return accountPath;
    }

    /**
     * @param accountPath the accountPath to set
     */
    public void setAccountPath(String accountPath) {
        this.accountPath = accountPath;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * @param profile the profile to set
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
        this.hostName = profile.getServer().getHost();
        this.userName = profile.getUserName();
        this.password = profile.getUserPassword();
        this.accountPath = profile.getAccount().getPath();
    }    

    /**
     * @return the session
     */
    public UniSession getSession() {
        return session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(UniSession session) {
        this.session = session;
    }
}
