/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.util;

/**
 *
 * @author rlittle
 */
public interface Progress {

    /**
     *
     * @param message
     * 
     */
    public void display(String message);

    /**
     *
     * @param message
     */
    public void state(String message);
    public void updateProgressBar(Double p);
    public void updateLed(String host, boolean onOff);
}
