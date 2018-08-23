/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;

/**
 *
 * @author rlittle
 */
public interface App {
    public void setup(Program program, Profile readProfile, Profile writeProfile, String[] criteria) throws Exception;
    public boolean mainLoop() throws Exception;
    public void teardown() throws Exception;
}
