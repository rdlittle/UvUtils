/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.u2.Uv;
import com.webfront.u2.client.UvClient;
import com.webfront.u2.model.Profile;
import com.webfront.u2.model.UvData;
import com.webfront.u2.util.Config;
import com.webfront.u2.util.Progress;
import com.webfront.uvtool.app.UvTool;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class CopyViewController implements Controller, Initializable, Progress {

    @FXML
    Button btnAddSourceProfile;
    @FXML
    Button btnAddDestProfile;

    @FXML
    Button btnCancel;
    @FXML
    Button btnCopy;

    @FXML
    ComboBox<Profile> cbSourceProfile;
    @FXML
    ComboBox<Profile> cbDestProfile;

    @FXML
    Circle sourceLed;

    @FXML
    Circle destLed;

    @FXML
    Label lblCriteria;
    @FXML
    Label lblStatusMessage;

    @FXML
    TextArea txtCriteria;
    @FXML
    TextArea txtOutput;

    @FXML
    TextField txtDestFile;
    @FXML
    TextField txtDestField;
    @FXML
    TextField txtDestValue;

    @FXML
    TextField txtSourceFile;
    @FXML
    TextField txtSourceField;
    @FXML
    TextField txtSourceValue;

    @FXML
    ProgressBar progressBar;

    @FXML
    RadioButton rbFromSavedList;
    @FXML
    RadioButton rbFromQuery;
    @FXML
    RadioButton rbReplace;
    @FXML
    RadioButton rbPreserve;
    @FXML
    RadioButton rbCreate;
    @FXML
    RadioButton rbIgnore;
    @FXML
    RadioButton rbSelectFromSource;
    @FXML
    RadioButton rbSelectFromDest;

    @FXML
    ToggleGroup tgDestExisting;
    @FXML
    ToggleGroup tgDestMissing;
    @FXML
    ToggleGroup tgSelectFrom;
    @FXML
    ToggleGroup tgSourceItems;

    private final Config config;

    SimpleObjectProperty<Profile> sourceProfileProperty;
    SimpleObjectProperty<Profile> destProfileProperty;
    RadialGradient ledOff;
    RadialGradient ledOn;

    ResourceBundle res;

    List<Stop> stopsOn;
    List<Stop> stopsOff;

    private final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

    public CopyViewController() {
        config = Config.getInstance();
        stopsOn = new ArrayList<>();
        stopsOff = new ArrayList<>();
        sourceProfileProperty = new SimpleObjectProperty();
        destProfileProperty = new SimpleObjectProperty();

        btnAddDestProfile = new Button();
        btnAddSourceProfile = new Button();
        btnCancel = new Button();
        btnCopy = new Button();

        cbSourceProfile = new ComboBox<>();
        cbDestProfile = new ComboBox<>();

        destLed = new Circle();
        lblCriteria = new Label();
        lblStatusMessage = new Label();

        stopsOn.add(new Stop(0, Color.web("#26ff6B")));
        stopsOn.add(new Stop(1.0, Color.web("#1e6824")));
        ledOn = new RadialGradient(0, -0.02, 0.51, 0.5, 0.97, true, CycleMethod.NO_CYCLE, stopsOn);

        stopsOff.add(new Stop(0, Color.web("#cccccc")));
        stopsOn.add(new Stop(1.0, Color.web("#1e6824")));
        ledOff = new RadialGradient(0, -0.02, 0.51, 0.5, 0.67, true, CycleMethod.NO_CYCLE, stopsOff);

        tgDestExisting = new ToggleGroup();
        tgDestMissing = new ToggleGroup();
        tgSourceItems = new ToggleGroup();

        txtCriteria = new TextArea();
        txtOutput = new TextArea();

        txtDestFile = new TextField();
        txtDestField = new TextField();
        txtDestValue = new TextField();

        txtSourceFile = new TextField();
        txtSourceField = new TextField();
        txtSourceValue = new TextField();

        progressBar = new ProgressBar();
        sourceLed = new Circle();

        rbCreate = new RadioButton();
        rbFromSavedList = new RadioButton();
        rbFromQuery = new RadioButton();
        rbIgnore = new RadioButton();
        rbPreserve = new RadioButton();
        rbReplace = new RadioButton();
        rbSelectFromDest = new RadioButton();
        rbSelectFromSource = new RadioButton();

        alert.setTitle("File name mismatch");
        alert.contentTextProperty().set("Destination file does not match source file!");
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
        cbSourceProfile.setItems(config.getProfiles());
        cbDestProfile.setItems(config.getProfiles());
        sourceProfileProperty.bind(cbSourceProfile.valueProperty());
        destProfileProperty.bind(cbDestProfile.valueProperty());
        tgSourceItems.selectedToggleProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (((RadioButton) newValue).getId().equals("rbFromSavedList")) {
                    lblCriteria.setText("List name");
                } else {
                    lblCriteria.setText("Selection criteria");
                }
            }
        });

        txtSourceFile.focusedProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (txtDestFile.getText().isEmpty()) {
                    txtDestFile.setText(txtSourceFile.getText());
                }
            }
        });
        txtSourceField.focusedProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (txtDestField.getText().isEmpty()) {
                    txtDestField.setText(txtSourceField.getText());
                }
            }
        });
        txtSourceValue.focusedProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (txtDestValue.getText().isEmpty()) {
                    txtDestValue.setText(txtSourceValue.getText());
                }
            }
        });
        rbFromQuery.selectedProperty().set(true);
        rbReplace.selectedProperty().set(true);
        rbCreate.selectedProperty().set(true);
        rbSelectFromSource.selectedProperty().set(true);
    }

    @FXML
    public void onAddProfile() {
        launch("viewProfile", "titleProfile");
    }

    /**
     *
     * @return
     */
    @Override
    public Button getCancelButton() {
        return btnCancel;
    }

    @FXML
    public void onCopy() {
        boolean proceed = true;
        if (!txtSourceFile.getText().equals(txtDestFile.getText())) {
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                proceed = false;
            }
        }
        if (proceed && validateForm()) {
            updateProgressBar(0D);
            String fileName = txtSourceFile.getText();
            String field = txtSourceField.getText();
            String value = txtSourceValue.getText();
            UvData source = new UvData(fileName, field, value);

            fileName = txtDestFile.getText();
            field = txtDestField.getText();
            value = txtDestValue.getText();
            UvData destination = new UvData(fileName, field, value);

            if (rbSelectFromDest.isSelected()) {
                destination.setSelectCriteria(txtCriteria.getText());
            } else {
                source.setSelectCriteria(txtCriteria.getText());
            }

            UvClient client = new UvClient(this);
            client.setSourceProfile(sourceProfileProperty.get());
            client.setDestProfile(destProfileProperty.get());
            client.setSourceData(source);
            client.setDestData(destination);
            if (rbFromQuery.isSelected()) {
                client.setSelectType(Uv.SelectType.QUERY);
            } else {
                client.setSelectType(Uv.SelectType.LIST);
            }
            if (rbReplace.isSelected()) {
                client.setExistingPolicy(Uv.Existing.OVERWRITE);
            } else {
                client.setExistingPolicy(Uv.Existing.PRESERVE);
            }
            if (rbCreate.isSelected()) {
                client.setMissingPolicy(Uv.Missing.CREATE);
            } else {
                client.setMissingPolicy(Uv.Missing.IGNORE);
            }
            if (rbSelectFromSource.isSelected()) {
                client.setSelectFrom(Uv.SelectFrom.SOURCE);
            } else {
                client.setSelectFrom(Uv.SelectFrom.DESTINATION);
            }
            Runnable task = () -> client.doCopy();
            Thread backgroundThread = new Thread(task);
            backgroundThread.setDaemon(true);
            backgroundThread.start();
        }
    }

    private boolean validateForm() {

        if (sourceProfileProperty.getValue() == null) {
            lblStatusMessage.setText(res.getString("errNoSourceProfile"));
            cbSourceProfile.requestFocus();
            return false;
        }
        if (destProfileProperty.getValue() == null) {
            lblStatusMessage.setText(res.getString("errNoDestProfile"));
            cbDestProfile.requestFocus();
            return false;
        }

        boolean hasDestField = !txtDestField.getText().isEmpty();
        boolean hasDestFile = !txtDestFile.getText().isEmpty();
        boolean hasDestValue = !txtDestValue.getText().isEmpty();

        boolean hasSourceFile = !txtSourceFile.getText().isEmpty();
        boolean hasSourceField = !txtSourceField.getText().isEmpty();
        boolean hasSourceValue = !txtSourceValue.getText().isEmpty();

        boolean destFieldNumeric = false;
        boolean destValueNumeric = false;

        boolean sourceFieldNumeric = false;
        boolean sourceValueNumeric = false;

        if (!hasSourceFile) {
            lblStatusMessage.setText(res.getString("lblSource") + " " + res.getString("errFileName"));
            txtSourceFile.requestFocus();
            return false;
        }
        if (!hasDestFile) {
            lblStatusMessage.setText(res.getString("lblDest") + " " + res.getString("errFileName"));
            txtDestFile.requestFocus();
            return false;
        }

        if (hasSourceField) {
            try {
                int f = Integer.parseInt(txtSourceField.getText(), 10);
                sourceFieldNumeric = true;
            } catch (NumberFormatException e) {
                if (hasDestField && txtDestField.getText().equals(txtSourceField.getText())) {
                    txtDestField.setText("");
                }
                lblStatusMessage.setText(res.getString("errNotNumeric"));
                txtSourceField.requestFocus();
                return false;
            }
        }
        if (hasSourceValue) {
            try {
                int f = Integer.parseInt(txtSourceValue.getText(), 10);
                sourceValueNumeric = true;
            } catch (NumberFormatException e) {
                if (hasDestValue && txtDestValue.getText().equals(txtSourceValue.getText())) {
                    txtDestValue.clear();
                }
                lblStatusMessage.setText(res.getString("errNotNumeric"));
                txtSourceValue.requestFocus();
                return false;
            }
        }

        if (hasDestField) {
            try {
                int f = Integer.parseInt(txtDestField.getText(), 10);
                destFieldNumeric = true;
            } catch (NumberFormatException e) {
                lblStatusMessage.setText(res.getString("errNotNumeric"));
                txtDestField.requestFocus();
                return false;
            }
        }
        if (hasDestValue) {
            try {
                int f = Integer.parseInt(txtDestValue.getText(), 10);
                destValueNumeric = true;
            } catch (NumberFormatException e) {
                lblStatusMessage.setText(res.getString("errNotNumeric"));
                txtDestValue.requestFocus();
                return false;
            }
        }

        if (hasSourceField != hasDestField) {
            lblStatusMessage.setText(res.getString("errUnmatchedField"));
            txtDestField.requestFocus();
            return false;
        }

        if (hasSourceValue != hasDestValue) {
            lblStatusMessage.setText(res.getString("errUnmatchedValue"));
            txtDestValue.requestFocus();
            return false;
        }

        if (hasSourceValue && !hasSourceField) {
            lblStatusMessage.setText(res.getString("errSourceFieldValue"));
            txtSourceField.requestFocus();
            return false;
        }

        if (hasDestValue && !hasDestField) {
            lblStatusMessage.setText(res.getString("errDestFieldValue"));
            txtDestField.requestFocus();
            return false;
        }

        lblStatusMessage.setText("");
        return true;
    }

    @Override
    public void launch(String view, String title) {
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
//            root.addEventFilter(KeyEvent.KEY_PRESSED, event -> System.out.println("Pressed: "+event.getCode()));
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

    @Override
    public void display(String message) {
        Platform.runLater(() -> txtOutput.appendText(message + "\n"));
    }

    @Override
    public void state(String message) {
        Platform.runLater(() -> txtOutput.appendText(message));
    }

    @Override
    public void updateProgressBar(Double p) {
        Platform.runLater(() -> progressBar.progressProperty().setValue(p));
    }

    @Override
    public void updateLed(String host, boolean onOff) {
        if (host.equalsIgnoreCase("source")) {
            sourceLed.setFill(onOff ? ledOn : ledOff);
        } else {
            destLed.setFill(onOff ? ledOn : ledOff);
        }
    }

}
