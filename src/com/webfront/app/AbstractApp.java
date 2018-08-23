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
public abstract class AbstractApp {
    
    public abstract void teardown();

    public abstract boolean mainLoop();

    public abstract void setup(Program program, Profile readProfile, Profile writeProfile);

}
