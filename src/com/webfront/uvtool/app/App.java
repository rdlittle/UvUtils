/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.app;

/**
 *
 * @author rlittle
 */
public interface App {
    public void setup();
    public boolean mainLoop();
    public void teardown();
}
