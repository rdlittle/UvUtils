/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.controller;

import com.webfront.u2.model.Program;
import com.webfront.u2.model.Prompt;
import com.webfront.u2.model.UvFile;
import com.webfront.u2.util.Config;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * FXML Controller class
 *
 * @author rlittle
 */
public class ProgramController implements Controller, Initializable {

    @FXML
    Button btnAddRow;

    @FXML
    Button btnCancel;

    @FXML
    Button btnDelete;

    @FXML
    Button btnDeleteRow;

    @FXML
    Button btnSave;

    @FXML
    CheckBox chkIsSubroutine;
    
    @FXML
    CheckBox chkIsNew;

    @FXML
    ComboBox<Program> cbAppSelector;

    @FXML
    TableView<Prompt> tblInputs;

    @FXML
    TableColumn tblColInputNumber;

    @FXML
    TableColumn<Prompt, String> tblColPrompt;

    @FXML
    TextArea txtDescription;

    @FXML
    TextArea txtReadFiles;

    @FXML
    TextArea txtWriteFiles;

    @FXML
    TextField txtAppName;

    @FXML
    TextField txtPackage;

    private final Config config = Config.getInstance();
    SimpleBooleanProperty rowSelected;
    SimpleBooleanProperty changed;

    public ProgramController() {
        chkIsNew = new CheckBox();
        chkIsSubroutine = new CheckBox();
        cbAppSelector = new ComboBox<>();
        txtAppName = new TextField();
        txtDescription = new TextArea();
        txtPackage = new TextField();
        txtReadFiles = new TextArea();
        txtWriteFiles = new TextArea();
        tblInputs = new TableView<>();
        tblColInputNumber = new TableColumn();
        tblColPrompt = new TableColumn<>();
        rowSelected = new SimpleBooleanProperty(false);
        changed = new SimpleBooleanProperty(false);
    }

    @Override
    public void launch(String v, String t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Button getCancelButton() {
        return btnCancel;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbAppSelector.setItems(config.getPrograms());
        cbAppSelector.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cbAppSelector.editableProperty().set(false);
                event.consume();
            } else {
                if (event.getCode() == KeyCode.ENTER) {
                    Program p = cbAppSelector.getValue();
                    event.consume();
                }
            };
        });
        cbAppSelector.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            int clicks = event.getClickCount();
            if (clicks == 2) {
                if (!cbAppSelector.editableProperty().get()) {
                    cbAppSelector.setEditable(true);
                }
            }
        });

        chkIsSubroutine.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler() {
            @Override
            public void handle(Event event) {
                changed.set(true);
            }
        });

        tblColInputNumber.setCellValueFactory(new PropertyValueFactory<>("num"));

        tblColPrompt.setCellValueFactory(new PropertyValueFactory<>("message"));
        tblColPrompt.setCellFactory(TextFieldTableCell.<Prompt>forTableColumn());
        tblColPrompt.setOnEditCommit((CellEditEvent<Prompt, String> t) -> {
            Prompt p = t.getRowValue();
            int row = tblColPrompt.getTableView().getEditingCell().getRow();
            p.setMessage(t.getNewValue());
        });

        tblInputs.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Prompt>() {
            @Override
            public void changed(ObservableValue<? extends Prompt> observable, Prompt oldValue, Prompt newValue) {
                rowSelected.set(newValue != null);
            }
        });

        if(chkIsNew.selectedProperty().get()) {
            changed.set(true);
        }
        btnDeleteRow.disableProperty().bind(rowSelected.not());
        btnSave.disableProperty().bind(changed.not());
    }

    @FXML
    public void onBtnDelete() {
        Program p = cbAppSelector.getValue();
        if (p == null) {
            return;
        }
        config.deleteProgram(p);
        txtPackage.setText("");
        txtReadFiles.clear();
        txtWriteFiles.clear();
        chkIsSubroutine.selectedProperty().set(false);
    }

    @FXML
    public void onBtnSave() {
        Object o = cbAppSelector.getValue();
        boolean isNew = true;
        Program p;
        if (o == null || o instanceof String) {
            String desc = txtDescription.getText();
            p = new Program();
            p.setName(txtAppName.getText());
            p.setClassName(txtPackage.getText());
            p.setDescription(desc == null ? "" : txtDescription.getText());
            p.setSubroutine(chkIsSubroutine.isSelected());
            int appId = config.addProgram(p);
            if (appId == -1) {
                return;
            }
            p.setId(appId);
        } else {
            p = cbAppSelector.getValue();
            isNew = false;
        }

        String[] rdFiles = txtReadFiles.getText().split("\n");
        String[] wrFiles = txtWriteFiles.getText().split("\n");
        
        if (!txtAppName.getText().isEmpty()) {
            p.setName(txtAppName.getText());
        }
        p.setClassName(txtPackage.getText());
        p.setDescription(txtDescription.getText());
        p.setSubroutine(chkIsSubroutine.isSelected());
        ArrayList<UvFile> fileList = new ArrayList<>();

        if (rdFiles.length > 0) {
            for (String s : rdFiles) {
                if (s.isEmpty()) {
                    continue;
                }
                fileList.add(new UvFile(p.getId(), s, true, false));
            }
        }
        if (wrFiles.length > 0) {
            for (String s : wrFiles) {
                if (s.isEmpty()) {
                    continue;
                }
                fileList.add(new UvFile(p.getId(), s, false, true));
            }
        }

        p.setFileList(fileList);
        p.getPrompts().clear();
        for (Prompt prp : tblInputs.getItems()) {
            int pNum = prp.getNum();
            String msg = prp.getMessage();
            p.getPrompts().put(pNum, prp);
        }

        if (isNew) {
            if (fileList.size() > 0) {
                config.addFiles(fileList);
            }
        } else {
            config.updateProgram(p);
        }
        cbAppSelector.setEditable(false);
        changed.set(false);
    }

    @FXML
    public void onAddRow() {
        Object o = cbAppSelector.getValue();
        Prompt p = new Prompt();
        int nextPrompt = tblInputs.getItems().size() + 1;
        p.setNum(nextPrompt);
        tblInputs.getItems().add(p);
    }

    @FXML
    public void onDeleteRow() {
        Prompt p = tblInputs.getSelectionModel().getSelectedItem();
        Program program = (Program) cbAppSelector.getValue();
        program.getPromptList().remove(p);
        program.getPrompts().remove(p.getNum());
        changed.set(true);
    }
    
    @FXML
    public void onDescriptionKeyTyped() {
        changed.set(true);
    }

    @FXML
    public void onAppSelect() {
        changed.set(false);
        Object o = cbAppSelector.getValue();
        if (o instanceof String) {
            return;
        }
        Program p = cbAppSelector.getValue();
        if (p == null) {
            return;
        }
        txtPackage.setText(p.getClassName());
        txtDescription.setText(p.getDescription());
        txtReadFiles.clear();
        txtWriteFiles.clear();

        for (UvFile uvf : p.getFileList()) {
            String fname = uvf.getFileName();
            if (uvf.isRead()) {
                txtReadFiles.appendText(fname + "\n");
            }
            if (uvf.isWrite()) {
                txtWriteFiles.appendText(fname + "\n");
            }
        }
        tblInputs.setItems(p.getPromptList());
        chkIsSubroutine.selectedProperty().set(p.isSubroutine());

    }

    @FXML
    public void onEditCancel() {
        changed.set(false);
    }

    @FXML
    public void onEditCommit() {
        Prompt p = new Prompt();
        int row = tblColPrompt.getTableView().getEditingCell().getRow();
        String msg = tblColPrompt.getCellData(row);
        p.setNum(row);
        tblColPrompt.getTableView().getItems().get(row).getMessage();
        changed.set(true);
    }

    @FXML
    public void onEditStart() {
        changed.set(true);
    }

}
