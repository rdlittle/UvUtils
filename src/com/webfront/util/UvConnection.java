/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniobjects.UniJava;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import com.webfront.u2.model.Profile;

/**
 *
 * @author rlittle
 */
public class UvConnection {

    private String userName;
    private String password;
    private String server;
    private String path;

    private UniJava uv = null;
    private static UvConnection uvc = null;

    protected UvConnection() {
        uv = new UniJava();
    }

    public static UvConnection getInstance() {
        if (uvc == null) {
            uvc = new UvConnection();
        }
        return uvc;
    }

    public static UniSession newSession(String u, String pass, String srv, String path) throws UniSessionException {
        UniSession uSession = UvConnection.getInstance().uv.openSession();
        uSession.setUserName(u);
        uSession.setPassword(pass);
        uSession.setHostName(srv);
        uSession.setAccountPath(path);
        return uSession;
    }

    public static UniSession newSession(Profile p) throws UniSessionException {
        UniSession uSession = UvConnection.getInstance().uv.openSession();
        uSession.setUserName(p.getUserName());
        uSession.setPassword(p.getUserPassword());
        uSession.setHostName(p.getServer().getHost());
        uSession.setAccountPath(p.getAccount().getPath());
        return uSession;
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
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

}
