/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.uvtool.util;

import com.webfront.u2.model.Program;
import com.webfront.u2.model.Prompt;
import java.util.HashMap;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

/**
 *
 * @author rlittle
 */
public class PromptDialog extends Dialog<HashMap<Integer,String>> {

    Program program;
    Ilist iList;

    public PromptDialog(Program program, Ilist il) {
        this.program = program;
        this.iList = il;
        setTitle(program.getName());
        int gridRows = program.getPromptList().size();
        GridPane grid = new GridPane();
        for (int p = 1; p <= gridRows; p++) {
            Prompt prp = program.getPrompts().get(p);
            String msg = prp.getMessage();
            grid.add(new Label(msg), 1, p);
            grid.add(new TextField(), 2, p);
        }
        getDialogPane().setContent(grid);
        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().add(buttonTypeOk);
        
        setResultConverter(new Callback<ButtonType,HashMap<Integer,String>>() {
            @Override
            public HashMap<Integer,String> call(ButtonType param) {
                if(param == buttonTypeOk) {
                    HashMap<Integer,String> response = new HashMap<>();
                    for(Node node : grid.getChildren()) {
                        int row = grid.getRowIndex(node);
                        int col = grid.getColumnIndex(node);
                        if(col==2) {
                            TextField txt = (TextField) node;
                            String resp = txt.getText();
                            resp=resp.toUpperCase();
                            response.put(Integer.valueOf(row), resp);
                        }
                    }
                    iList.setIlist(response);
                    return response;
                }
                return null;
            }
        });
    }

}
