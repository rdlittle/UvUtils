/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.u2.model.Server;
import com.webfront.u2.util.Config;
import com.webfront.u2.util.ServerConverter;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class ServerViewController implements Controller, Initializable {

    @FXML
    Button btnSave;
    @FXML
    Button btnCancel;

    @FXML
    ComboBox<Server> cbServer;

    @FXML
    TextField txtHostName;

    @FXML
    Label lblStatusMessage;

    ResourceBundle res;

    public ServerViewController() {
        btnCancel = new Button();
        btnSave = new Button();
        cbServer = new ComboBox<>();
        cbServer.converterProperty().set(new ServerConverter());

        lblStatusMessage = new Label();
        txtHostName = new TextField();
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.res = rb;
        txtHostName.setPromptText("Enter host name");
        cbServer.setItems(Config.getInstance().getServers());
        cbServer.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                Server s = (Server) newValue;
                txtHostName.setText(s.getHost());
            }
        });
        cbServer.requestFocus();
    }

    @FXML
    public void btnSaveOnAction() {
        Server selectedServer = cbServer.getValue();
        String serverName = selectedServer.getName();
        String hostName = selectedServer.getHost();
        if (serverName.isEmpty()) {
            lblStatusMessage.setText(res.getString("errNoServerName"));
            cbServer.requestFocus();
        } else if (hostName.isEmpty()) {
            lblStatusMessage.setText(res.getString("errNoHostName"));
            txtHostName.requestFocus();
        } else {
            Config cfg = Config.getInstance();
            lblStatusMessage.setText("");
            if (cfg.getServer(serverName) != null) {
                selectedServer.setHost(txtHostName.getText());
                cfg.updateServer(selectedServer);
            } else {
                Server s = new Server();
                s.setHost(hostName);
                s.setName(serverName);
                cfg.addServer(s);
            }
            txtHostName.clear();
            btnCancel.fire();
        }
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
