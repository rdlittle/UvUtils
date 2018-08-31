/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import asjava.uniobjects.UniSessionException;
import com.webfront.u2.Uv;
import com.webfront.u2.model.Account;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Server;
import com.webfront.u2.model.User;
import com.webfront.u2.util.AccountConverter;
import com.webfront.u2.util.Config;
import com.webfront.u2.util.ProfileConverter;
import com.webfront.u2.util.ServerConverter;
import com.webfront.uvtool.app.UvTool;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class ProfileController implements Controller {

    @FXML
    private URL location;
    @FXML
    private ResourceBundle resources;
    @FXML
    private Button btnAddAccount;
    @FXML
    private Button btnAddHost;
    @FXML
    private Button btnAddProfile;
    @FXML
    private Button btnAddServer;
    @FXML
    private Button btnTest;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;

    @FXML
    private ComboBox<Account> cbAccounts;
    @FXML
    private ComboBox<Profile> cbProfiles;
    @FXML
    private ComboBox<Server> cbServers;

    @FXML
    private ImageView imgSpinner;

    @FXML
    private TextField txtHostName;
    @FXML
    private TextField txtPath;
    @FXML
    private TextField txtUserName;

    @FXML
    private PasswordField pwPassword;

    @FXML
    private Label lblStatusMessage;
    @FXML
    private Label lblProfile;

    private Image img;

    public SimpleObjectProperty<Profile> profile;
    private SimpleBooleanProperty isNew;
    private SimpleBooleanProperty isPasswordChange;
    private final Config config = Config.getInstance();
    Profile selectedProfile;
    private FilteredList<Server> filteredServerList;
    private FilteredList<Account> filteredAccountList;

    public ProfileController() {
    }

    @FXML
    public void initialize() {
        selectedProfile = new Profile();
        filteredServerList = new FilteredList<>(config.getServers());
        filteredServerList.setPredicate((e) -> true);

        filteredAccountList = new FilteredList<>(config.getAccounts());
        filteredAccountList.setPredicate((e) -> true);

        lblStatusMessage.setText("");
        isNew = new SimpleBooleanProperty(true);
        isPasswordChange = new SimpleBooleanProperty(false);
        imgSpinner.setImage(new Image("/com/webfront/uvtool/image/loading.gif"));
        imgSpinner.setVisible(false);
        btnSave.disableProperty().set(true);
        DropShadow shadow = new DropShadow(1, 6, 6, Color.LIGHTGRAY);
        shadow.setBlurType(BlurType.GAUSSIAN);

        cbAccounts.converterProperty().set(new AccountConverter());
        cbAccounts.setItems(filteredAccountList);

        cbProfiles.converterProperty().set(new ProfileConverter());
        cbProfiles.setItems(config.getProfiles());

        cbProfiles.setEffect(shadow);
        cbServers.converterProperty().set(new ServerConverter());
        cbServers.setItems(config.getServers());
        cbServers.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cbServers.editableProperty().set(false);
                event.consume();
            } else {
                if (event.getCode() == KeyCode.ENTER) {
                    Server s = cbServers.getValue();
                    event.consume();
                }
            };
        });
        cbServers.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                Server s = (Server) newValue;
                filteredServerList.setPredicate((v) -> s == null || s.getName().length() == 0 || v.getName().startsWith(s.getName()));
            }
        });

        txtPath.textProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                String prevValue = (String) oldValue;
                if (prevValue != null && !prevValue.isEmpty()) {
                    btnSave.disableProperty().set(false);
                }
            }
        });

        txtUserName.textProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                String prevValue = (String) oldValue;
                if (prevValue != null && !prevValue.isEmpty()) {
                    btnSave.disableProperty().set(false);
                }
            }
        });

        pwPassword.textProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                String prevValue = (String) oldValue;
                if (prevValue != null && !prevValue.isEmpty()) {
                    btnSave.disableProperty().set(false);
                    isPasswordChange.set(true);
                }
            }
        });

        btnTest.disableProperty().bind(cbServers.valueProperty().isNull().or(cbAccounts.valueProperty().isNull()).or(txtUserName.textProperty().isEmpty()).or(pwPassword.textProperty().isEmpty()));

    }

    @FXML
    void btnTestOnAction() {
        String h = txtHostName.getText();
        String u = txtUserName.getText();
        String pw = pwPassword.getText();
        Account a = cbAccounts.getValue();
        String p = txtPath.getText();
        if (h.isEmpty()) {
            lblStatusMessage.setText(resources.getString("serverNameRequired"));
        }
        lblStatusMessage.setText(resources.getString("statusConnecting"));
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                btnSave.disableProperty().set(true);
                Uv uv = Uv.newInstance(h, u, pw, p);

                try {
                    imgSpinner.setVisible(true);
//                    btnTest.disableProperty().set(true);
                    if (uv.connect()) {
                        lblStatusMessage.setText(resources.getString("statusConnectionSucceeded"));
                        btnSave.disableProperty().set(false);
                        if (isNew.get()) {
                            cbProfiles.editableProperty().set(true);
                            cbProfiles.requestFocus();
                        }
                    } else {
                        lblStatusMessage.setText(resources.getString("statusConnectionFailed"));
                    }
                    imgSpinner.setVisible(false);
//                    btnTest.disableProperty().set(false);
                } catch (UniSessionException ex) {
                    String s = ex.getMessage();
                    lblStatusMessage.setText(s);
                    imgSpinner.setVisible(false);
//                    btnTest.disableProperty().set(false);
                }
            }
        });
    }

    @FXML
    void btnSaveOnAction() {
        lblStatusMessage.setText("");
        updateModel();
        Profile p = (Profile) cbProfiles.getSelectionModel().getSelectedItem();
        if (p == null) {
            lblStatusMessage.setText(resources.getString("msgProfileTitle"));
            isNew.set(true);
            cbProfiles.editableProperty().set(true);
            cbProfiles.requestFocus();
        } else if (!isNew.getValue()) {
            try {
                config.updateProfile(p);
                if (p.getAccount() != selectedProfile.getAccount()) {
                    if (p.getAccountId() <= 0) {
                        config.addAccount(selectedProfile.getAccount());
                    } else {
                        config.updateAccount(selectedProfile.getAccount());
                    }
                }
                if (p.getUser() != selectedProfile.getUser() || isPasswordChange.get()) {
                    selectedProfile.getUser().setPassword(pwPassword.getText());
                    config.updateUser(selectedProfile.getUser());
                    isPasswordChange.setValue(false);
                }
                if (p.getServer() != selectedProfile.getServer()) {
                    config.updateServer(selectedProfile.getServer());
                }
                lblStatusMessage.setText("Profile saved");
                btnSave.disableProperty().set(true);
            } catch (SQLException e) {
                lblStatusMessage.setText(e.getMessage());
            }
        } else {
            lblStatusMessage.setText("");
            Server s = new Server();
            Account a = new Account();
            User u = new User();
            if (cbServers.getValue() != null) {
                s = cbServers.getValue();
            }
            if (cbAccounts.getValue() != null) {
                a = cbAccounts.getValue();
            }
            u.setName(txtUserName.getText());
            u.setPassword(pwPassword.getText());
            for (User user : config.getUsers()) {
                if (user.getName().equals(u.getName())) {
                    if (user.getPassword().equals(u.getPassword())) {
                        u = user;
                        break;
                    }
                }
            }
            Integer userId = Integer.valueOf(u.getId());
            if (userId == -1) {
                int uid = config.addUser(u);
                u.setId(uid);
            }
            p.setAccount(a);
            p.setServer(s);
            u.setName(txtUserName.getText());
            u.setPassword(pwPassword.getText());
            p.setUser(u);
            config.addProfile(p);
            lblStatusMessage.setText(resources.getString("msgProfileCreated"));
        }
    }

    @FXML
    public void cbAccountsOnAction() {
        Account account = cbAccounts.getValue();
        if (account != null) {
            txtPath.setText(account.getPath());
        }
    }

    @FXML
    public void cbServersOnAction() {
        Server s = (Server) cbServers.getValue();
        if (s == null) {
            txtHostName.clear();
        } else {
            txtHostName.setText(s.getHost());
            filteredAccountList.setPredicate((a) -> (s.getName().equals(a.getServerName())));
        }
    }

    @FXML
    public void cbProfilesOnAction() {
        selectedProfile = cbProfiles.getValue();
        if (cbProfiles.isEditable()) {
            cbProfiles.setEditable(false);
        } else {
            if (selectedProfile != null) {
                setFormData();
                isNew.set(false);
            } else {
                isNew.set(true);
            }
        }
    }

    @FXML
    public void btnAddServerOnAction() {
        cbServers.editableProperty().set(true);
        txtHostName.clear();
        cbServers.requestFocus();
//        cbServers.getEditor().setOnKeyPressed(new EventHandler<KeyEvent>() {
//            @Override
//            public void handle(KeyEvent event) {
//                KeyCode key = event.getCode();
//                System.out.println("key pressed: " + key);
//                if (key == KeyCode.ESCAPE) {
//                    System.out.println("ESC key pressed");
//                    txtHostName.requestFocus();
//                    cbServers.editableProperty().set(false);
//                } else if (key == KeyCode.ENTER) {
//                    System.out.println("Enter key pressed");
//                }
//            }
//        });
//        cbServers.getEditor().setOnKeyTyped(new EventHandler<KeyEvent>() {
//            @Override
//            public void handle(KeyEvent event) {
//                KeyCode key = event.getCode();
//                System.out.println("key typed: " + key);
//                if (key == KeyCode.ESCAPE) {
//                    System.out.println("ESC key typed");
//                    txtHostName.requestFocus();
//                    cbServers.editableProperty().set(false);
//                } else if (key == KeyCode.ENTER) {
//                    System.out.println("Enter key typed");
//                    cbServers.editableProperty().set(false);
//                }
//            }
//        });

//        launch("viewServer","titleServer");
    }

    @FXML
    public void cbServersOnKeyTyped(KeyEvent event) {
        KeyCode key = event.getCode();
        System.out.println("key typed: " + key);
        if (key == KeyCode.ESCAPE) {
            System.out.println("ESC key typed");
            txtHostName.requestFocus();
            cbServers.editableProperty().set(false);
        } else if (key == KeyCode.ENTER) {
            System.out.println("Enter key typed");
            cbServers.editableProperty().set(false);
        }
    }

    @FXML
    public void btnAddAccountOnAction() {

    }

    public void setFormData() {
        cbServers.getSelectionModel().select(selectedProfile.getServer());
        cbAccounts.getSelectionModel().select(selectedProfile.getAccount());
        txtUserName.setText(selectedProfile.getUserName());
        txtHostName.setText(selectedProfile.getServer().getHost());
        pwPassword.setText(selectedProfile.getUserPassword());
    }

    public void updateModel() {
        Account a = cbAccounts.getValue();
        Server s = cbServers.getValue();
        User u;
        if (isPasswordChange.get()) {
            u = selectedProfile.getUser();
        } else {
            u = config.getUser(txtUserName.getText(), pwPassword.getText());
            if (u == null) {
                u = new User(txtUserName.getText(), pwPassword.getText());
                u.setId(config.addUser(u));
            }
        }

        a.setPath(txtPath.getText());
        s.setHost(txtHostName.getText());

        if (selectedProfile == null) {
            selectedProfile = new Profile();
        }
        selectedProfile.setAccount(cbAccounts.getValue());
        selectedProfile.setServer(cbServers.getValue());
        selectedProfile.setUser(u);
    }

    @Override
    public Button getCancelButton() {
        return btnCancel;
    }

    @FXML
    public void btnAddProfileOnAction() {
        selectedProfile = new Profile();
        cbProfiles.setValue(null);
        cbAccounts.setValue(null);
        cbServers.setValue(null);
        txtHostName.clear();
        txtPath.clear();
        txtUserName.clear();
        pwPassword.clear();
        isNew.set(true);
    }

    @Override
    public void launch(String view, String title) {
        FXMLLoader viewLoader = new FXMLLoader();
        String v = resources.getString(view);
        String t = resources.getString(title);
        URL url = UvTool.class.getResource(v);
        viewLoader.setLocation(url);
        viewLoader.setResources(resources);
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

    /**
     * @return the btnTest
     */
    public Button getBtnTest() {
        return btnTest;
    }

}
