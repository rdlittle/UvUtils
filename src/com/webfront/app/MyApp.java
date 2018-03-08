/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.u2.model.Prompt;

/**
 *
 * @author rlittle
 */
public class MyApp extends AbstractApp {

    @Override
    public void setup(Program program, Profile readProfile, Profile writeProfile) {
        for(Prompt p : program.getPrompts().values()) {
            String msg = p.getMessage();
            System.out.println(p.getNum()+": "+msg);
        }
    }

    @Override
    public boolean mainLoop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void teardown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
