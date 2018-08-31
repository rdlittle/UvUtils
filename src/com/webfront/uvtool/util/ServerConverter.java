/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.util;

import com.webfront.uvtool.model.Server;
import javafx.util.StringConverter;

/**
 *
 * @author rlittle
 */
public class ServerConverter  extends StringConverter<Server> {
        @Override
        public String toString(Server object) {
            if(object==null) {
                return "";
            }
            return object.getName();
        }

        @Override
        public Server fromString(String string) {
            for(Server s : Config.getInstance().getServers()) {
                if(string.equalsIgnoreCase(s.getName())) {
                    return s;
                }
            }
            return new Server();
        }
    
}
