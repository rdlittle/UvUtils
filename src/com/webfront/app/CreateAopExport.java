/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.app;

import asjava.uniclientlibs.UniDynArray;
import static asjava.uniclientlibs.UniTokens.UVE_RNF;
import com.webfront.u2.model.Prompt;
import com.webfront.u2.model.UvData;
import com.webfront.util.FileUtils;

/**
 *
 * @author rlittle
 */
public class CreateAopExport extends BaseApp {

    @Override
    public boolean mainLoop() {
        String aopUploadId = "";
        String vendorId = "";
        String internalDate = "";
        String internalTime = "";
        String userId = "";
        String[] dt = getDateTime();
        UvData aopUploadRec = new UvData();
        UniDynArray aopExportRec;
        int result;
        for (Prompt prp : program.getPromptList()) {
            if (prp.getMessage().equalsIgnoreCase("AOP.UPLOAD id")) {
                aopUploadId = prp.getResponse();
            }
            if (prp.getMessage().equalsIgnoreCase("Vendor id")) {
                vendorId = prp.getResponse();
            }
            if (prp.getMessage().equalsIgnoreCase("Internal date")) {
                internalDate = prp.getResponse();
            }
            if (prp.getMessage().equalsIgnoreCase("Internal time")) {
                internalTime = prp.getResponse();
            }
            if (prp.getMessage().equalsIgnoreCase("User id")) {
                userId = prp.getResponse();
            }
        }
        if (aopUploadId.isEmpty()) {
            progress.display("AOP.UPLOAD id is required");
            teardown();
            return false;
        }
        if (vendorId.isEmpty()) {
            progress.display("Vendor id is required");
            teardown();
            return false;
        }
        if (userId.isEmpty()) {
            progress.display("User id is required");
            teardown();
            return false;
        }
        if (internalDate.isEmpty()) {
            internalDate = dt[0];
        }
        if (internalTime.isEmpty()) {
            internalTime = dt[1];
        }
        aopUploadRec = new UvData();
        aopUploadRec.setId(aopUploadId);
        result = FileUtils.getRecord(readFiles.get("AOP.UPLOAD"), aopUploadRec);
        if (result == UVE_RNF) {
            progress.display("AOP.UPLOAD " + aopUploadId + " not found");
            teardown();
            return false;
        }
        int attrs = aopUploadRec.getData().dcount();
        Double itemCount = new Double(attrs);
        Double itemsDone = new Double(0D);
        Double pctDone = new Double(0D);
        UvData data = new UvData();
        for (int nextSeq = 1; nextSeq <= attrs; nextSeq++) {
            String text = aopUploadRec.getData().extract(nextSeq).toString();
            String aopExportId = Integer.toString(nextSeq) + "*" + userId + "*" + vendorId + "*" + internalDate + "*" + internalTime;
            data.setId(aopExportId);
            data.setData(toDynArray(text));
            result = FileUtils.writeRecord(writeFiles.get("AOP.EXPORT"), data);
            itemsDone++;
            pctDone = itemsDone / itemCount;
            progress.display(itemsDone.intValue() + " of " + itemCount.intValue() + " : " + (pctDone.intValue() * 100) + "%");
            progress.updateProgressBar(pctDone);
        }
        teardown();
        return true;
    }

    private UniDynArray toDynArray(String str) {
        UniDynArray uda = new UniDynArray();
        String[] array = str.split(",");
        int sz = array.length;
        for (int i = 0; i < sz; i++) {
            uda.replace(i, array[i]);
        }
        return uda;
    }
}
