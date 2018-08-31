/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.util;

import java.util.HashMap;

/**
 *
 * @author rlittle
 */
public interface Wrapper {
    public void setIlist(HashMap<Integer,String> iList);
    public void setOlist(HashMap<Integer,String> oList);
    public void setElist(HashMap<Integer,String> eList);
    public HashMap<Integer,String> getIlist();
    public HashMap<Integer,String> getOlist();
    public HashMap<Integer,String> getElist();
    public void setSubName(String name);
    public String getSubName();
    public void call();
}
