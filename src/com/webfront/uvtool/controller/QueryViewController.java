/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.u2.util.Progress;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class QueryViewController implements Controller, Initializable, Progress {

    @FXML
    Button btnCancel;
    @FXML
    Button btnRun;
    
    @FXML
    TextArea taCriteria;
    
    ResourceBundle resources;
    URL location;
    
    public QueryViewController() {
        btnCancel = new Button();
        btnRun = new Button();
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        location = url;
        resources = rb;
    }    

    @Override
    public Button getCancelButton() {
        return btnCancel;
    }
    
    @FXML
    public void onRun() {
        String v = resources.getString("viewOutput");
        String t = resources.getString("titleOutput");
        launch(v,t);
    }

    @Override
    public void launch(String v, String t) {
        
    }

    @Override
    public void display(String message) {
        
    }

    @Override
    public void state(String message) {

    }
    
    @Override
    public void updateProgressBar(Double d) {
        
    }

    @Override
    public void updateLed(String host, boolean onOff) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
