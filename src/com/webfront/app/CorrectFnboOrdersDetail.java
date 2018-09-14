/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import com.webfront.exception.NotFoundException;
import com.webfront.exception.RecordLockException;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class CorrectFnboOrdersDetail extends BaseApp {

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST of ORDERS.DETAIL ids is required");
            teardown();
            return false;
        }
        try {
            UniSelectList list3 = readSession.selectList(3);
            list3.getList(listName);
            UniDynArray temp = list3.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            list3.clearList();
            list3 = getList(readSession, listName);
            while (!list3.isLastRecordRead()) {
                String maOrderId = list3.next().toString();
                if (maOrderId.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                Double pctDoneText = pctDone * 100;
                progress.display(maOrderId);
                progress.updateProgressBar(pctDone);
                
                // get ORDERS.DETAIL rec
                UvData ordersDetailRec = null;
                try {
                    ordersDetailRec = getOrdersDetail(maOrderId);
                    UniDynArray uda = ordersDetailRec.getData();
                    uda.insert(65, " ");
                    uda.replace(66, uda.extract(65));
                    uda.replace(67, uda.extract(53));
                    uda.replace(68, uda.extract(54));
                    uda.replace(69, uda.extract(55));
                    uda.replace(53, "0");
                    uda.replace(54, "0");
                    uda.replace(55, "0");
                    uda.replace(65, "");
                    ordersDetailRec.setData(uda);
                    writeOrdersDetail(ordersDetailRec);
                } catch (NotFoundException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RecordLockException ex) {
                    Logger.getLogger(RecalcIbv.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (UniSelectListException e) {

        } catch (UniSessionException ex) {
            Logger.getLogger(CorrectFnboOrdersDetail.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    private UvData getOrdersDetail(String orderId) throws NotFoundException, RecordLockException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result = FileUtils.lockRecord(writeFiles.get("ORDERS.DETAIL"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.unlockRecord(writeFiles.get("ORDERS.DETAIL"), uvData);
            return null;
        }
        return uvData;
    }

    void releaseOrdersDetail(UvData rec) {
        if (rec != null) {
            FileUtils.unlockRecord(writeFiles.get("ORDERS.DETAIL"), rec);
        }
    }

    void writeOrdersDetail(UvData ordersDetailRec) {
        if (ordersDetailRec != null) {
            int result = FileUtils.writeRecord(writeFiles.get("ORDERS.DETAIL"), ordersDetailRec);
        }
    }

}
