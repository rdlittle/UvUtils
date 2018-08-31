/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.model;

import java.util.Comparator;

/**
 *
 * @author rlittle
 * create table servers (name char(16), host char(128))
 */
public class Server {
    private String name;
    private String host;

    public Server() {
        name = "";
        host = "";
    }
    
    public Server(String n, String h) {
       name = n;
       host = h;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (name.isEmpty() ? name.hashCode() : 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other==null) {
            return false;
        }
        if(!(other instanceof Server)) {
            return false;
        }
        Server s = (Server) other;
        if(!s.name.equalsIgnoreCase(this.name)) {
            return false;
        }
        return true;
    }
    
    public static Comparator<Server> ServerComparator = new Comparator<Server>() {
        @Override
        public int compare(Server o1, Server o2) {
            return o1.name.compareTo(o2.name);
        }
    };
    
}
