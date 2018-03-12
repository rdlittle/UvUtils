/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_NOERROR;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import static asjava.uniobjects.UniObjectsTokens.LOCK_NO_LOCK;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import com.webfront.u2.model.Prompt;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class CreateAopQueueFromAo extends BaseApp {

    boolean isHistory;
    UvData aopQueueRec;
    int runLevel = -1;
    String queueType;
    String queueGroup;
    UvData aoRec;

    @Override
    public boolean mainLoop() {
        try {
            for (Prompt prp : program.getPromptList()) {
                if (prp.getMessage().equalsIgnoreCase("Run level")) {
                    runLevel = Integer.parseInt(prp.getResponse());
                }
                if (prp.getMessage().equalsIgnoreCase("Queue type")) {
                    queueType = prp.getResponse();
                }
                if (prp.getMessage().equalsIgnoreCase("Queue group")) {
                    queueGroup = prp.getResponse();
                }
            }
            if (runLevel == -1) {
                progress.display("Run level is required");
                return false;
            }
            if (queueType.isEmpty()) {
                progress.display("Queue type is required");
                return false;
            }
            if (queueGroup.isEmpty()) {
                progress.display("Queue group is required");
                return false;
            }
            UniSelectList list3 = readSession.selectList(3);
            list3.getList(listName);
            UniDynArray temp = list3.readList();
            Double itemCount = new Double(temp.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            UniSelectList list = getList(readSession, listName);
            ArrayList<String> orkList = new ArrayList<>();
            ArrayList<String> keyList = new ArrayList<>();
            while (!list.isLastRecordRead()) {
                String id = list.next().toString();
                if (id.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                progress.display(itemsDone.intValue() + " of " + itemCount.intValue() + " : " + (pctDone.intValue() * 100) + "%");
                progress.updateProgressBar(pctDone);
                aoRec = new UvData();
                aoRec.setId(id);
                if (!findAo(aoRec)) {
                    progress.display("Can't find AO " + id);
                    continue;
                }
                String orkId = aoRec.getData().extract(197).toString();
                if (orkList.contains(orkId)) {
                    continue;
                }
                orkList.add(orkId);

                String vendorId = aoRec.getData().extract(181, 1).toString();
                String[] tmp = orkId.split("\\*");
                String key = vendorId + "*" + tmp[1] + '*' + tmp[2];
                if (keyList.contains(key)) {
                    continue;
                }
                keyList.add(key);

                if (!getNextQueue()) {
                    progress.display("Can't get AOP.QUEUE record");
                    continue;
                }

                aopQueueRec.getData().replace(1, vendorId);
                aopQueueRec.getData().replace(4, Integer.toString(runLevel));
                aopQueueRec.getData().replace(5, tmp[1]);
                aopQueueRec.getData().replace(6, tmp[2]);
                aopQueueRec.getData().replace(7, "AOPSUPPORT");
                aopQueueRec.getData().replace(14, queueType);
                aopQueueRec.getData().replace(20, queueGroup);
                for (int i = 0; i < runLevel; i++) {
                    aopQueueRec.getData().insert(24, 1, i);
                    aopQueueRec.getData().insert(25, 1, tmp[1]);
                    aopQueueRec.getData().insert(26, 1, tmp[2]);
                    aopQueueRec.getData().insert(27, 1, tmp[1]);
                    aopQueueRec.getData().insert(28, 1, tmp[2]);
                    aopQueueRec.getData().insert(29, 1, "0");
                }

                int result = FileUtils.writeRecord(writeFiles.get("AOP.QUEUE"), aopQueueRec);
            }
            teardown();
        } catch (UniSessionException ex) {
            Logger.getLogger(CreateAopQueueFromAo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(CreateAopQueueFromAo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public boolean findAo(UvData record) {
        isHistory = false;
        int result;
        result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), record);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                result = FileUtils.getRecord(readFiles.get("AOP.HIST"), record);
                switch (result) {
                    case -1:
                        return false;
                    case UVE_NOERROR:
                        isHistory = true;
                        break;
                    default:
                        return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    public boolean getNextQueue() {
        int result;
        UvData seqRec = new UvData();
        seqRec.setId("AOP.QUEUE$SEQ.ID");
        int nextQueue = 1000;
        result = FileUtils.lockRecord(readFiles.get("SEQ.FILE"), seqRec);
        switch (result) {
            case LOCK_NO_LOCK:
                return false;
            case UVE_RNF:
                break;
        }
        result = FileUtils.getRecord(readFiles.get("SEQ.FILE"), seqRec);
        String queueId = seqRec.getData().extract(1).toString();
        nextQueue = Integer.parseInt(queueId);

        seqRec.getData().replace(1, Integer.toString(nextQueue + 1));
        result = FileUtils.writeRecord(writeFiles.get("SEQ.FILE"), seqRec);

        aopQueueRec = new UvData();
        aopQueueRec.setId(queueId);
        result = FileUtils.lockRecord(readFiles.get("AOP.QUEUE"), aopQueueRec);

        if (result != UVE_RNF) {
            result = FileUtils.unlockRecord(readFiles.get("AOP.QUEUE"), aoRec);
            return false;
        }
        return true;
    }

}
