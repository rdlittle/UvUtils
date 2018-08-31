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
 * create table accounts (id integer primary key autoincrement, server char(16), name char(128), path char(256))
 */
public class Account {
    private int id;
    private String serverName;
    private String name;
    private String path;

    public Account() {
        id = -1;
        serverName = "";
        name = "";
        path = "";
    }
    public Account(int aid, String s, String n, String p) {
        id = aid;
        serverName = s;
        name = n;
        path = p;
    }
    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the serverId
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @param name the serverName to set
     */
    public void setServerName(String name) {
        this.serverName = name;
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
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        Integer h = Integer.valueOf(id);
        hash += (h != null ? h.hashCode() : 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other==null) {
            return false;
        }
        if(!(other instanceof Account)) {
            return false;
        }
        Account a = (Account) other;
        if(a.id != this.id) {
            return false;
        }
        return true;
    }
    
    public static Comparator<Account> ServerComparator = new Comparator<Account>() {
        @Override
        public int compare(Account o1, Account o2) {
            String acct1 = o1.name+o1.path;
            String acct2 = o2.name+o2.path;
            return acct1.compareTo(acct2);
        }
    };    
}
