/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniDynArray;

/**
 *
 * @author rlittle
 */
public class UvData {

    private String fileName;
    private String id;
    private int field;
    private int value;
    private String selectCriteria;
    private UniDynArray data;

    public UvData() {
        fileName = "";
        id = "";
        field = -1;
        value = -1;
        selectCriteria = "";
        data = new UniDynArray();
    }

    public UvData(String fName) {
        this();
        fileName = fName;
    }

    public UvData(String fName, String fNum) {
        this();
        fileName = fName;
        if (!fNum.isEmpty()) {
            field = Integer.parseInt(fNum);
        }        
    }

    public UvData(String fName, String fNum, String fVal) {
        this();
        fileName = fName;
        if (!fNum.isEmpty()) {
            field = Integer.parseInt(fNum);
        }
        if (!fVal.isEmpty()) {
            value = Integer.parseInt(fVal);
        }
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the fieldNum
     */
    public String getFieldAsString() {
        return Integer.toString(field);
    }

    public int getField() {
        return field;
    }

    /**
     * @return the fieldValue
     */
    public int getValue() {
        return value;
    }

    /**
     * @return the fieldValue
     */
    public String getValueAsString() {
        return Integer.toString(value);
    }

    /**
     * @return the selectCriteria
     */
    public String getSelectCriteria() {
        return selectCriteria;
    }

    /**
     * @param selectCriteria the selectCriteria to set
     */
    public void setSelectCriteria(String selectCriteria) {
        this.selectCriteria = selectCriteria;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the data
     */
    public UniDynArray getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(UniDynArray data) {
        this.data = data;
    }
}
