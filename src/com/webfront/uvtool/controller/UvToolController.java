/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.u2.model.Account;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Server;
import com.webfront.u2.model.User;
import com.webfront.u2.util.Config;
import com.webfront.u2.util.ServerConverter;
import com.webfront.uvtool.app.UvTool;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author rlittle
 */
public class UvToolController implements Initializable {

    private final Config config;
    private final ObservableList<Account> accountList = FXCollections.observableArrayList();
    private final ObservableList<Profile> profileList = FXCollections.observableArrayList();
    private final ObservableList<Server> serverList = FXCollections.<Server>observableArrayList();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final ObservableList<String> programList = FXCollections.observableArrayList();
    
    ResourceBundle res;
    
    @FXML
    ComboBox cbServers;
    @FXML
    ComboBox cbProfiles;

    @FXML
    Label statusMessage;
    
    @FXML
    Button btnRun;
    @FXML
    Button btnQuery;
    @FXML
    Button btnEdit;
    @FXML
    Button btnCopy;

    @FXML
    MenuItem fileExit;
    @FXML
    MenuItem mnuFileNewAccount;
    @FXML
    MenuItem mnuFileNewServer;
    @FXML
    MenuItem mnuFileNewProfile;
    @FXML
    MenuItem mnuFileNewProgram;
    @FXML
    MenuItem mnuEditAccount;
    @FXML
    MenuItem mnuEditServer;
    @FXML
    MenuItem mnuEditProfile;  
    @FXML
    MenuItem mnuEditApp;

    public UvToolController() {
        config = Config.getInstance();
        accountList.setAll(config.getAccounts());
        profileList.setAll(config.getProfiles());
        serverList.setAll(config.getServers());
        serverList.sort(Server.ServerComparator);
        userList.setAll(config.getUsers());
        cbServers = new ComboBox<>();
        cbProfiles = new ComboBox<>();
        btnRun = new Button();
        btnQuery = new Button();
        btnEdit = new Button();
        btnCopy = new Button();
        mnuFileNewAccount = new MenuItem();
        mnuFileNewServer = new MenuItem();
        mnuFileNewProfile = new MenuItem();
        mnuFileNewProgram = new MenuItem();
        mnuEditAccount = new MenuItem();
        mnuEditServer = new MenuItem();
        mnuEditProfile = new MenuItem();
        mnuEditApp = new MenuItem();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        res = rb;
        cbServers.converterProperty().setValue(new ServerConverter());
        cbServers.getItems().addAll(serverList);
        
        cbServers.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
//                btnEditServer.disableProperty().set(newValue.toString().equalsIgnoreCase("Select"));
            }
        });
        
        btnCopy.setOnAction(event -> launch("viewCopy","titleCopy"));        
        btnCopy.addEventHandler(MouseEvent.MOUSE_ENTERED, new MouseOver());
        btnCopy.addEventHandler(MouseEvent.MOUSE_EXITED, new MouseOut());         

        btnEdit.setOnAction(event ->launch("viewEdit","titleEdit"));
        btnEdit.addEventHandler(MouseEvent.MOUSE_ENTERED, new MouseOver());
        btnEdit.addEventHandler(MouseEvent.MOUSE_EXITED, new MouseOut());        
        
        btnQuery.setOnAction(event -> launch("viewQuery","titleQuery"));
        btnQuery.addEventHandler(MouseEvent.MOUSE_ENTERED, new MouseOver());
        btnQuery.addEventHandler(MouseEvent.MOUSE_EXITED, new MouseOut());
        
        btnRun.setOnAction(event ->launch("viewRun","titleRun"));
        btnRun.addEventHandler(MouseEvent.MOUSE_ENTERED, new MouseOver());
        btnRun.addEventHandler(MouseEvent.MOUSE_EXITED, new MouseOut());
       
    }

    public MenuItem getFileExit() {
        return fileExit;
    }
    
    private void launch(String view, String title) {
        FXMLLoader viewLoader = new FXMLLoader();
        String v = res.getString(view);
        String t = res.getString(title);
        URL url = UvTool.class.getResource(v);
        viewLoader.setLocation(url);
        viewLoader.setResources(res);
        try {
            Pane root = viewLoader.<Pane>load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle(t);
            Controller ctrl = viewLoader.getController();
            ctrl.getCancelButton().setOnAction(new EventHandler() {
                @Override
                public void handle(Event event) {
                   ctrl.getCancelButton().removeEventHandler(EventType.ROOT, this);
                   stage.close();
                }
            });
            
            stage.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(UvToolController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    @FXML
    public void displayMessage(String msg) {
        statusMessage.setText(msg);
    }
    
    @FXML
    public void removeMessage() {
        statusMessage.setText("");
    }
    
    class MouseOver implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            Button src = (Button) event.getSource();
            String id = src.getId();
            String msg = res.getString(id);
            displayMessage(msg);
        }
    }
    
    class MouseOut implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            removeMessage();
        }
    } 

    @FXML
    public void onFileNewAccount() {
        launch("viewAccount","titleAccount");
    }
    
    @FXML
    public void onFileNewServer() {
        launch("viewServer","titleServer");
    }    
    
    @FXML
    public void onFileNewProfile() {
        launch("viewProfile","titleProfile");
    } 
    
    @FXML
    public void onFileNewProgram() {
        launch("viewProgram","titleProgram");
    }
    
    @FXML
    public void onEditAccount() {
        launch("viewAccount","titleAccount");
    }
    
    @FXML
    public void onEditServer() {
        launch("viewServer","titleServer");
    }    
    
    @FXML
    public void onEditProfile() {
        launch("viewProfile","titleProfile");
    }
    
    @FXML
    public void onEditApp() {
        launch("viewAppEdit","titleAppEdit");
    }

}
