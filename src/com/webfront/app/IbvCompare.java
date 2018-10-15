/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniException;
import asjava.uniclientlibs.UniStringException;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.exception.NotFoundException;
import com.webfront.u2.model.UvData;
import com.webfront.u2.util.AffiliateOrder;
import com.webfront.u2.util.MaOrder;
import com.webfront.util.CalcIbvCashback;
import com.webfront.util.ExchangeRate;
import com.webfront.util.SysUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author rlittle
 */
public class IbvCompare extends BaseApp {
   private ExchangeRate rates;
    private final AffiliateOrder aoReader = new AffiliateOrder(readFiles);
    private final MaOrder orderReader = new MaOrder(readFiles);

    private int svrStatus;
    private String svrMessage;
    private String svrCtrlCode;

    private String newIbv;
    private UniDynArray aoIds;
    private UniDynArray report;
    private CalcIbvCashback calculator;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST of AFFILIATE.ORDERS ids is required");
            teardown();
            return false;
        }
        try {
            aoIds = new UniDynArray();
            report = new UniDynArray();
            rates = new ExchangeRate(readSession);
            calculator = new CalcIbvCashback(readSession);
            UniSelectList list = readSession.selectList(3);
            list.getList(listName);
            UniDynArray temp = list.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            list = readSession.selectList(0);
            list.getList(listName);
            UniDynArray recList = list.readList();
            for (int attr = 1; attr <= itemCount; attr++) {
                String aoId = recList.extract(attr).toString();
                String orderId;
                itemsDone += 1;
                pctDone = itemsDone / itemCount;
                progress.updateProgressBar(pctDone);
                progress.display(aoId);
                if (aoId.isEmpty()) {
                    continue;
                }
                UvData aoRec;
                UvData orderRec;
                try {
                    aoRec = aoReader.getAo(aoId);
                    if (aoRec.getData().extract(30).toString().isEmpty()) {
                        continue;
                    }
                    orderId = aoRec.getData().extract(30).toString();
                    orderRec = orderReader.getOrder(orderId);
                    String oldIbv = Integer.toString(SysUtils.sum(orderRec.getData().extract(152)));
                    calculator.calc(aoRec);
                    newIbv = calculator.getIbvTotal();
                    if (newIbv.equals(oldIbv)) {
                        continue;
                    }
                    int oldibv = Integer.parseInt(oldIbv);
                    int newibv = Integer.parseInt(newIbv);
                    int diff = Math.abs(oldibv - newibv);
                    if (diff < 3) {
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(aoId);
                    sb.append("\t" + orderId + "\t" + oldIbv + "\t" + newIbv);
                    report.insert(-1, sb.toString());
                } catch (NotFoundException ex) {
                    Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UniSubroutineException ex) {
                    Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UniStringException ex) {
                    Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (UniSessionException ex) {
            Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (report.dcount() > 0) {
            SaveTask save = new SaveTask(getStage());
            save.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                    new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    try {
                        File outputFile;
                        outputFile = save.getValue();
                        String rpt = report.toString().replaceAll(SysUtils.asString(254), "\n");
                        FileWriter writer = new FileWriter(outputFile);
                        writer.write(rpt);
                        writer.flush();
                        writer.close();
                    } catch (IOException ex) {
                        Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            Thread t = new Thread(save);
            Platform.runLater(t);
        }
        teardown();
        Platform.runLater(() -> getStage().close());
        return true;
    }

    class SaveTask extends Task<File> {

        private Stage stage;

        public SaveTask(Stage s) {
            this.stage = s;
        }

        @Override
        protected File call() throws Exception {
            final File file;
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(new File(System.getProperty("user.home")));
            file = fc.showSaveDialog(stage);
            return file;
        }

    }
}
