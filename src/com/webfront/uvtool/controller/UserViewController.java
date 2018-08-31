/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class UserViewController implements Controller, Initializable {

    @FXML
    Button btnCancel;
    @FXML
    Button btnSave;
    @FXML
    TextField txtName;
    @FXML
    PasswordField pwPassword;
    @FXML
    ComboBox cbUsers;

    public UserViewController() {
        btnCancel = new Button();
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    
    @Override
    public Button getCancelButton() {
        return btnCancel;
    }     

    @Override
    public void launch(String v, String t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
