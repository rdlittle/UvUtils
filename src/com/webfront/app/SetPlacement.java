package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import asjava.uniobjects.UniCommand;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniJava;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetPlacement extends BaseApp {

//    UniJava uv;
//    UniFile localFile;
    UniSubroutine remoteSub;
//    UniCommand cmd;
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
            for(int ptr = 1; ptr <= itemCount; ptr ++) {
                String aoId = recList.extract(ptr).toString();
                if (aoId.isEmpty()) {
                    continue;
                }
                itemsDone++;
                pctDone = itemsDone / itemCount;
                Double pctDoneText = pctDone * 100;
                progress.display(aoId);
                progress.updateProgressBar(pctDone);

                UvData aoRec = new UvData();
                aoRec.setId(aoId);
                if (!findAo(aoRec)) {
                    progress.display("Can't find AO " + aoId);
                    continue;
                }
                String payingId = aoRec.getData().extract(8).toString();
                String orderDate = aoRec.getData().extract(161).toString();
                String placement = callSub(payingId, orderDate);
                if (!placement.isEmpty()) {
                    aoRec.getData().replace(151, placement);
                    int result = FileUtils.writeRecord(writeFiles.get("AFFILIATE.ORDERS"), aoRec);
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

    public boolean findAo(UvData record) {
        int result;
        isHistory = false;
        result = FileUtils.getRecord(readFiles.get("AFFILIATE.ORDERS"), record);
        switch (result) {
            case -1:
                return false;
            case UVE_RNF:
                return false;
        }
        return true;
    }

    public static void main(String[] args) {
//        SetPlacement sp = new SetPlacement();
//        if (!sp.connect()) {
//            System.out.println("Can't connect");
//        } else {
//            try {
//                sp.remoteSub = readSession.subroutine("SET.BV.AUTO.PLACEMENT.OP", 3);
//                sp.localFile = localSession.openFile("AFFILIATE.ORDERS");
//                sp.list = sp.doSelect();
//                UniSelectList list3 = readSession.selectList(3);
//                list3.getList(listName);
//                UniDynArray temp = list3.readList();
//                Double itemCount = new Double(temp.dcount());
//                Double itemsDone = new Double(0);
//                Double pctDone = new Double(0);
//                list3.clearList();
//                list3 = getList(readSession, listName);
//                int recordCount = 0;
//                while (!sp.list.isLastRecordRead()) {
//                    try {
//                        String recordId = sp.list.next().toString();
//                        if (recordId != null && !recordId.isEmpty()) {
//                            UniDynArray aoRec = new UniDynArray(sp.localFile.read(recordId));
//                            String payingId = aoRec.extract(8).toString();
//                            String orderDate = aoRec.extract(161).toString();
//                            String placement = sp.callSub(payingId, orderDate);
//                            if (!placement.isEmpty()) {
//                                recordCount++;
//                                aoRec.replace(151, placement);
//                                sp.localFile.write(recordId, aoRec);
//                                System.out.println(Integer.toString(recordCount) + "  " + recordId);
//
//                            }
//                        }
//                    } catch (UniFileException ex) {
//                        Logger.getLogger(SetPlacement.class
//                                .getName()).log(Level.SEVERE, null, ex);
//
//                    } catch (UniSelectListException ex) {
//                        Logger.getLogger(SetPlacement.class
//                                .getName()).log(Level.SEVERE, null, ex);
//
//                    } catch (UniSubroutineException ex) {
//                        Logger.getLogger(SetPlacement.class
//                                .getName()).log(Level.SEVERE, null, ex);
//
//                    }
//                }
//            } catch (UniSessionException ex) {
//                Logger.getLogger(SetPlacement.class
//                        .getName()).log(Level.SEVERE, null, ex);
//            }
//        }
    }

}
