/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.uvtool.model.Account;
import com.webfront.uvtool.model.Server;
import com.webfront.uvtool.util.AccountConverter;
import com.webfront.uvtool.util.Config;
import com.webfront.uvtool.util.ServerConverter;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class AccountViewController implements Controller, Initializable {

    @FXML
    Button btnSave;
    @FXML
    Button btnCancel;
    @FXML
    Button btnDelete;
    @FXML
    Label lblStatusMessage;
    @FXML
    TextField txtPath;
    @FXML
    ComboBox<Account> cbAccount;
    @FXML
    ComboBox<Server> cbServers;

    ResourceBundle res;
    private final Config config = Config.getInstance();
    private final FilteredList<Account> filteredAccountList;
    private Account selectedAccount;

    /**
     * Initializes the controller class.
     */
    public AccountViewController() {
        filteredAccountList = new FilteredList<>(config.getAccounts());
        filteredAccountList.setPredicate((e) -> true);
        selectedAccount = new Account();

        btnSave = new Button();
        btnCancel = new Button();
        lblStatusMessage = new Label();
        txtPath = new TextField();
        cbAccount = new ComboBox<>();
        cbAccount.converterProperty().set(new AccountConverter());

        cbServers = new ComboBox<>();
        cbServers.converterProperty().set(new ServerConverter());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        res = rb;
        cbAccount.setItems(filteredAccountList);
        cbAccount.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue instanceof Account) {
                    Account a = (Account) newValue;
                    selectedAccount.setName(a.getName());
                    txtPath.setText(a.getPath());
                }
            }
        });

        cbAccount.addEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter -> {
            if (eventFilter.getClickCount() == 2) {
                cbAccount.editableProperty().set(true);
                cbAccount.getEditor().requestFocus();
            }
        });

        cbAccount.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cbAccount.editableProperty().set(false);
                event.consume();
            } else {
                if (event.getCode() == KeyCode.ENTER) {
                    if (cbAccount.getEditor() != null) {
                        String act = cbAccount.getEditor().getText();
                        Server s = cbServers.getValue();
                        if (filteredAccountList.size() == 0) {
                            Account tAcct = Config.getInstance().getAccountByName(act, s.getName());
                            if (tAcct == null) {
                                selectedAccount = new Account();
                                selectedAccount.setServerName(s.getName());
                                selectedAccount.setName(act);
                                cbAccount.setValue(selectedAccount);
                            } else {
                                selectedAccount = tAcct;
                            }
                        } else {
                            for (Account a : filteredAccountList) {
                                if (a.getServerName().equalsIgnoreCase(s.getName())) {
                                    selectedAccount.setName(act);
                                }
                            }
                        }
                    }
                    cbAccount.editableProperty().set(false);
                    event.consume();
                }
            };
        });

        cbAccount.focusedProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                boolean isFocused = (boolean) newValue;
                if (isFocused) {
                    return;
                }
                if (cbAccount.getEditor() != null) {
                    String act = cbAccount.getEditor().getText();
                    Server s = cbServers.getValue();
                    if (filteredAccountList.size() == 0) {
                        Account tAcct = Config.getInstance().getAccountByName(act, s.getName());
                        if (tAcct == null) {
                            selectedAccount = new Account();
                            selectedAccount.setServerName(s.getName());
                            selectedAccount.setName(act);
                        } else {
                            selectedAccount = tAcct;
                        }
                    } else {
                        for (Account a : filteredAccountList) {
                            if (a.getServerName().equalsIgnoreCase(s.getName())) {
                                if(!a.getName().equalsIgnoreCase(act)) {
                                    selectedAccount.setName(act);
                                    selectedAccount.setPath("");
                                    txtPath.setText("");
                                }
                            }
                        }
                    }
                }
                cbAccount.setValue(selectedAccount);
                cbAccount.editableProperty().set(false);
            }
        });

        cbServers.getItems().addAll(config.getServers());
        cbServers.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                Server s = (Server) newValue;
                String serverName = s.getName();
                filteredAccountList.setPredicate((a) -> a == null || a.getName().length() == 0 || a.getServerName().equalsIgnoreCase(serverName));
                selectedAccount = new Account();
                txtPath.setText("");
                cbAccount.valueProperty().set(selectedAccount);
            }
        });

        txtPath.textProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (oldValue.equals(newValue)) {
                    return;
                }
                selectedAccount.setPath((String) newValue);
            }
        });

    }

    @FXML
    public void btnSaveOnAction() {
        Server server = cbServers.getValue();
        Account acct = cbAccount.getValue();
        String name = acct.getName();
        String path = txtPath.getText();
        if (server == null) {
            lblStatusMessage.setText(res.getString("errServerSelect"));
            return;
        } else if (name.isEmpty()) {
            lblStatusMessage.setText(res.getString("errNoAccountPath"));
            return;
        } else if (path.isEmpty()) {
            lblStatusMessage.setText(res.getString("errNoPath"));
            return;
        }
        for (Account a : config.getAccounts()) {
            if (name.equals(a.getName()) && a.getServerName().equalsIgnoreCase(server.getName())) {
                config.updateAccount(a);
                return;
            }
        }
        Account account = new Account();
        account.setServerName(server.getName());
        account.setName(name);
        account.setPath(path);
        config.addAccount(account);
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
