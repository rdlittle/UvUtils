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
public class VirtualBatch implements Wrapper {
    
    private final String listName;
    private final String tclStatement;
    private HashMap<Integer,String> iList;
    private HashMap<Integer,String> oList;
    private HashMap<Integer,String> eList;
    private String subName;

    public static enum InputType {
        FROM_LIST, FROM_QUERY;
    }
    InputType inputType;
    
    private final String[] fileName;
    
    public VirtualBatch() {
        fileName = new String[10];
        listName = "";
        tclStatement = "";
        iList = new HashMap<>();
        oList = new HashMap<>();
        eList = new HashMap<>();
    }
    
    public void setArg() {
        
    }
    
    public InputType getInputType() {
        return inputType;
    }
    
    public void setInputType(InputType t) {
        inputType = t;
    }
    
    @Override
    public void call() {
        
    }

    @Override
    public String getSubName() {
        return subName;
    }

    @Override
    public void setSubName(String name) {
        subName = name;
    }

    @Override
    public HashMap<Integer, String> getElist() {
        return eList;
    }

    @Override
    public HashMap<Integer, String> getOlist() {
        return oList;
    }

    @Override
    public HashMap<Integer, String> getIlist() {
        return iList;
    }

    @Override
    public void setElist(HashMap<Integer, String> el) {
        eList = el;
    }

    @Override
    public void setOlist(HashMap<Integer, String> ol) {
        oList = ol;
    }

    @Override
    public void setIlist(HashMap<Integer, String> il) {
        iList = il;
    }
    
    public void setIlistValue(Integer i, String s) {
        iList.put(i, s);
    }
    
    public String getOlistValue(Integer i) {
        return oList.get(i);
    }
    
}
