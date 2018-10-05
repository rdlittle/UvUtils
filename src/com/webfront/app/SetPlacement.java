package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import static asjava.uniobjects.UniObjectsTokens.LOCK_NO_LOCK;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.exception.NotFoundException;
import com.webfront.exception.RecordLockException;
import com.webfront.u2.model.UvData;
import com.webfront.u2.util.FileUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetPlacement extends BaseApp {

    UniSubroutine remoteSub;
    boolean isHistory;

    @Override
    public boolean mainLoop() {
        if (listName.isEmpty()) {
            progress.display("SELECTLIST of AFFILIATE.ORDERS ids is required");
            teardown();
            return false;
        }
        try {
            remoteSub = readSession.subroutine("SET.BV.AUTO.PLACEMENT.OP", 3);
        } catch (UniSessionException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            UniSelectList list = readSession.selectList(3);
            list.getList(listName);
            UniDynArray recList = list.readList();
            Double itemCount = new Double(recList.dcount());
            Double itemsDone = new Double(0);
            Double pctDone = new Double(0);
            for (int ptr = 1; ptr <= itemCount; ptr++) {
                String aoId = recList.extract(ptr).toString();
                if (aoId.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                Double pctDoneText = pctDone * 100;
                progress.display(aoId);
                progress.updateProgressBar(pctDone);

                UvData aoRec = null;
                UvData ordRelRec = null;
                try {
                    aoRec = getAffiliateOrder(aoId);
                } catch (NotFoundException ex) {
                    Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                } catch (RecordLockException ex) {
                    Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }

                String ordId = aoRec.getData().extract(30).toString();
                boolean hasOrdersRelease = false;
                if (!ordId.isEmpty()) {
                    try {
                        ordRelRec = getOrdersRelease(ordId);
                        hasOrdersRelease = true;
                    } catch (NotFoundException ex) {
                        Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                        int result = FileUtils.unlockRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
                        continue;
                    } catch (RecordLockException ex) {
                        Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
                        int result = FileUtils.unlockRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
                        continue;
                    }
                }

                String payingId = aoRec.getData().extract(8).toString();
                String orderDate = aoRec.getData().extract(161).toString();
                String placement = callSub(payingId, orderDate);
                if (placement.isEmpty()) {
                    int result = FileUtils.unlockRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
                    if(hasOrdersRelease) {
                        result = FileUtils.unlockRecord(writeFiles.get("ORDERS.RELEASE"), ordRelRec);
                    }
                    continue;
                }
                aoRec.getData().replace(151, placement);
                int result = FileUtils.writeRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
                if (hasOrdersRelease) {
                    ordRelRec.getData().replace(151, placement);
                    result = FileUtils.writeRecord(writeFiles.get("ORDERS.RELEASE"), ordRelRec);
                }
            }
        } catch (UniSelectListException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSessionException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSubroutineException ex) {
            Logger.getLogger(SetPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        teardown();
        return true;
    }

    public String callSub(String payingId, String orderDate) throws UniSubroutineException {
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();

        iList.insert(1, payingId);
        iList.insert(3, "U");
        iList.insert(4, "ENG");
        iList.insert(5, orderDate);
        iList.insert(6, "IBV");

        remoteSub.setArg(0, iList);
        remoteSub.setArg(1, oList);
        remoteSub.setArg(2, eList);
        remoteSub.call();

        eList = new UniDynArray(remoteSub.getArg(2));
        int svrStatus = Integer.parseInt(eList.extract(1).toString());
        String svrCtrlCode = eList.extract(5).toString();
        String svrMessage = eList.extract(2).toString();
        if (svrStatus == -1) {
            System.out.println("Error " + svrCtrlCode + " " + svrMessage);
            return "";
        }
        oList = new UniDynArray(remoteSub.getArg(1));
        return oList.extract(1).toString();
    }

    private UvData getAffiliateOrder(String aoId) throws NotFoundException, RecordLockException {
        UvData uvData = new UvData();
        uvData.setId(aoId);
        int result = FileUtils.lockRecord(writeFiles.get("AFFILIATE.ORDERS"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.unlockRecord(writeFiles.get("AFFILIATE.ORDERS"), uvData);
            return null;
        }
        return uvData;
    }

    private UvData getOrdersRelease(String orderId) throws NotFoundException, RecordLockException {
        UvData uvData = new UvData();
        uvData.setId(orderId);
        int result = FileUtils.lockRecord(writeFiles.get("ORDERS.RELEASE"), uvData);
        if (result == UVE_RNF) {
            result = FileUtils.unlockRecord(writeFiles.get("ORDERS.RELEASE"), uvData);
            return null;
        }
        return uvData;
    }
}
