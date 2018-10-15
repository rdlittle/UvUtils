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
public class CashbackCompare extends BaseApp {

    private ExchangeRate rates;
    private final AffiliateOrder aoReader = new AffiliateOrder(readFiles);
    private final MaOrder orderReader = new MaOrder(readFiles);

    private int svrStatus;
    private String svrMessage;
    private String svrCtrlCode;

    private String newCashback;
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
                    String oldCashback = orderRec.getData().extract(375).toString();
                    calculator.calc(aoRec);
                    newCashback = calculator.getCashbackTotal();
                    if (newCashback.equals(oldCashback)) {
                        continue;
                    }
                    int oldCb = Integer.parseInt(oldCashback);
                    int newCb = Integer.parseInt(newCashback);
                    int diff = Math.abs(oldCb - newCb);
                    if (diff < 3) {
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(aoId);
                    sb.append("\t" + orderId + "\t" + oldCashback + "\t" + newCashback);
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

    UniDynArray recalcCashback(UvData aoRec) throws UniSessionException, UniSubroutineException {
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();
        String aggId = aoRec.getData().extract(181, 1).toString();
        String storeId = aoRec.getData().extract(157, 1).toString();
        String ppcId = aoRec.getData().extract(8).toString();
        String orderDate = aoRec.getData().extract(161, 1).toString();

        iList.replace(4, aggId);
        iList.replace(5, storeId);
        iList.replace(6, ppcId);
        iList.replace(7, orderDate);
        iList.replace(8, aoRec.getData().extract(371));
        iList.replace(9, aoRec.getData().extract(173));
        iList.replace(10, aoRec.getData().extract(180));

        UniSubroutine subroutine = readSession.subroutine("getCashbackCalc.uvs", 3);
        subroutine.setArg(0, iList);
        subroutine.setArg(1, oList);
        subroutine.setArg(2, eList);
        subroutine.call();
        eList = subroutine.getArgDynArray(2);
        svrStatus = Integer.parseInt(eList.extract(1).toString());
        svrCtrlCode = eList.extract(5).toString();
        svrMessage = eList.extract(2).toString();
        if (svrStatus == -1) {
            return null;
        }
        oList = subroutine.getArgDynArray(1);
        return new UniDynArray(SysUtils.sum(oList.extract(1)));
    }

    public String applyExchangeRates(UvData aoRec) throws UniStringException {
        String hcc = aoRec.getData().extract(290).toString();
        String reportingCurrency = aoRec.getData().extract(368).toString();
        String iDate = aoRec.getData().extract(161).toString();
        String amount = readSession.oconv(newCashback, "mr2").toString();
        try {
            UniDynArray rateInfo = rates.getExchangeRateInfo("", hcc, reportingCurrency, "", iDate, amount);
            amount = readSession.iconv(rateInfo.extract(5).toString(), "mr22").toString();
        } catch (UniException ex) {
            Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        }
        return amount;

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
