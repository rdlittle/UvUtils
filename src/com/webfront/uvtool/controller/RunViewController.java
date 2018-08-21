/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.app.AbstractApp;
import com.webfront.app.BaseApp;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.Program;
import com.webfront.u2.model.Prompt;
import com.webfront.u2.util.Config;
import com.webfront.u2.util.Progress;
import com.webfront.uvtool.util.Ilist;
import com.webfront.uvtool.util.PromptDialog;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class RunViewController implements Controller, Progress, Initializable {

    @FXML
    CheckBox chkLockProfiles;

    @FXML
    Circle connectionLed;

    @FXML
    ComboBox<Profile> cbReadFrom;

    @FXML
    ComboBox<Profile> cbWriteTo;

    @FXML
    ComboBox<Program> cbAppName;

    @FXML
    TextField txtListName;

    @FXML
    TextArea txtCriteria;

    @FXML
    TextArea txtDescription;

    @FXML
    TextArea txtOutput;

    @FXML
    Button btnOk;

    @FXML
    Button btnCancel;

    @FXML
    ProgressBar pbProgress;

    private final Config config = Config.getInstance();
    ResourceBundle res;
    private AbstractApp app;
    private SimpleBooleanProperty lockProfilesProperty;

    List<Stop> stopsOn;
    List<Stop> stopsOff;
    RadialGradient ledOff;
    RadialGradient ledOn;

    Thread backgroundThread = null;
    final InputList iList = new InputList();

    public RunViewController() {
        stopsOn = new ArrayList<>();
        stopsOff = new ArrayList<>();
        btnCancel = new Button();
        btnOk = new Button();
        connectionLed = new Circle();
        chkLockProfiles = new CheckBox();
        cbReadFrom = new ComboBox<>();
        cbWriteTo = new ComboBox<>();
        cbAppName = new ComboBox<>();
        pbProgress = new ProgressBar();
        lockProfilesProperty = new SimpleBooleanProperty(true);
        txtListName = new TextField();
        txtOutput = new TextArea();
        txtCriteria = new TextArea();
        txtDescription = new TextArea();
        stopsOn.add(new Stop(0, Color.web("#26ff6B")));
        stopsOn.add(new Stop(1.0, Color.web("#1e6824")));
        ledOn = new RadialGradient(0, -0.02, 0.51, 0.5, 0.97, true, CycleMethod.NO_CYCLE, stopsOn);

        stopsOff.add(new Stop(0, Color.web("#cccccc")));
        stopsOn.add(new Stop(1.0, Color.web("#1e6824")));
        ledOff = new RadialGradient(0, -0.02, 0.51, 0.5, 0.67, true, CycleMethod.NO_CYCLE, stopsOff);

    }

    @Override
    public void display(String message) {
        Platform.runLater(() -> txtOutput.appendText(message + "\n"));
    }

    @Override
    public void state(String message) {
//        Platform.runLater(() -> txtOutput.appendText(message));
    }

    @Override
    public void updateProgressBar(Double p) {
        Platform.runLater(() -> pbProgress.progressProperty().setValue(p));
    }

    @Override
    public void updateLed(String host, boolean onOff) {
        connectionLed.setFill(onOff ? ledOn : ledOff);
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        res = rb;
        cbAppName.setItems(config.getPrograms());
        cbReadFrom.setItems(config.getProfiles());
        cbWriteTo.setItems(config.getProfiles());
        lockProfilesProperty.bind(chkLockProfiles.selectedProperty());
        cbWriteTo.disableProperty().bind(lockProfilesProperty);
        connectionLed.setFill(ledOff);
        btnCancel.addEventHandler(MouseEvent.MOUSE_RELEASED, new ButtonHandler());
    }

    @FXML
    public void exec() {
        try {
            Program p = cbAppName.getValue();
            PromptDialog dialog = new PromptDialog(p, iList);
            String appClassName = p.getClassName() + "." + p.getName();
            BaseApp a = (BaseApp) Class.forName(appClassName).newInstance();
            a.setProgress(this);
            Profile readProfile = cbReadFrom.getValue();
            Profile writeProfile = cbWriteTo.getValue();
            String[] criteria = txtCriteria.getText().split("\n");
            if (p.getPromptList().size() > 0) {
                dialog.showAndWait();
            }
            if (iList.getIlist() != null) {
                for (int i : iList.iList.keySet()) {
                    String r = iList.getIlist().get(i);
                    Prompt prp = p.getPrompts().get(i);
                    prp.setResponse(r);
                    p.getPrompts().put(i, prp);
                };
            }
            p.setListName(txtListName.getText());
            p.setSelectCriteria(criteria);
            a.setup(p, readProfile, writeProfile);
            Runnable task = () -> a.mainLoop();
            backgroundThread = new Thread(task);
            backgroundThread.setDaemon(true);
            backgroundThread.start();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RunViewController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(RunViewController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(RunViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    class ButtonHandler implements EventHandler {

        @Override
        public void handle(Event event) {
            Button btn = (Button) event.getSource();
            String id = btn.getId();
            if (id.equalsIgnoreCase("btnCancel")) {
                EventType type = event.getEventType();
                if (type == MOUSE_RELEASED && backgroundThread != null) {
                    if (backgroundThread.isAlive()) {
                        backgroundThread.interrupt();
                    }
                }
            }
        }
    }

    class InputList implements Ilist {

        private HashMap<Integer, String> iList;

        public InputList() {
            this.iList = new HashMap<>();
        }

        @Override
        public HashMap<Integer, String> getIlist() {
            return this.iList;
        }

        @Override
        public void setIlist(HashMap<Integer, String> iList) {
            this.iList = iList;
        }

    }

    @Override
    public void launch(String view, String title) {

    }

    @Override
    public Button getCancelButton() {
        return btnCancel;
    }

    @FXML
    public void onAppSelect() {
        txtDescription.setText(cbAppName.getValue().getDescription());
    }

}
