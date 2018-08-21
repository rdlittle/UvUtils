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
import javafx.scene.control.TextArea;

/**
 *
 * @author rlittle
 */

public class OutputWindowController implements Controller, Initializable {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextArea txtWindow;

    @FXML
    private Button btnClose;

    @FXML
    private Button btnSave;

    @FXML
    public void initialize(URL url, ResourceBundle rb) {
        location = url;
        resources = rb;
    }

    @Override
    public Button getCancelButton() {
        return btnClose;
    }

    @Override
    public void launch(String v, String t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

