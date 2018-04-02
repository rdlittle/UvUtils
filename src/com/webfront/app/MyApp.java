/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniobjects.UniCommandException;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class MyApp extends BaseApp {

    @Override
    public boolean mainLoop() {
        try {
            UniSelectList list = doSelect(readSession,program.getSelectCriteria());
            while(!list.isLastRecordRead()) {
                String itemId = list.next().toString();
                if(itemId.isEmpty()) {
                    continue;
                }
                System.out.println(list.next().toString());
            }
            teardown();
        } catch (UniSessionException ex) {
            Logger.getLogger(MyApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniCommandException ex) {
            Logger.getLogger(MyApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(MyApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

}
