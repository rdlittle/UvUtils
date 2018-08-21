/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.util;

import com.webfront.uvtool.model.Profile;
import javafx.util.StringConverter;

/**
 *
 * @author rlittle
 */
public class ProfileConverter extends StringConverter<Profile> {

    @Override
    public String toString(Profile object) {
        if(object == null) {
            return "";
        }
        return object.toString();
    }

    @Override
    public Profile fromString(String string) {
        for(Profile p : Config.getInstance().getProfiles()) {
            if(p.getProfileName().equals(string)) {
                return p;
            }
        }
        Profile p = new Profile();
        p.setProfileName(string);
        return p;
    }
    
}
