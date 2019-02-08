/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniStringException;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniJava;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.u2.model.UvData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class TestCashbackCompare {

    private File file;
    private UniFile aoFile;
    private CalcIbvCashback calc;
    String host;
    String user;
    String pass;
    String acct;

    public TestCashbackCompare() {

    }

    private void setup() throws FileNotFoundException, IOException {
        file = new File("//home/rlittle/credentials");
        BufferedReader buffer = new BufferedReader(new FileReader(file));
        String config = buffer.readLine();
        String[] params = config.split("\\s");
        host = params[0];
        user = params[1];
        pass = params[2];
        acct = params[3];
    }

    public static void main(String[] args) {
        TestCashbackCompare tester = new TestCashbackCompare();
        UniJava uv = new UniJava();
        try {
            tester.setup();
            UniSession session = new UniSession();
            session.connect(tester.host, tester.user, tester.pass, tester.acct);
            tester.calc = new CalcIbvCashback(session);
            tester.aoFile = session.open("AFFILIATE.ORDERS");
            UniSelectList list = session.selectList(0);
            list.getList("RDL.AO");
            while(!list.isLastRecordRead()) {
                String aoId = list.next().toString();
                if(aoId.isEmpty()) {
                    continue;
                }
                UvData aoRec = new UvData();
                aoRec.setId(aoId);
                tester.calc.calc(aoRec);
                String oldIbv = aoRec.getData().extract(149).toString();
                String newIbv = tester.calc.getIbvTotal();
                if (oldIbv.equals(newIbv)) {
                    System.out.println(aoId+" oldIbv: "+oldIbv+" newIbv: "+newIbv);
                }
            }
            session.disconnect();
        } catch (IOException ex) {
            Logger.getLogger(TestCashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSessionException ex) {
            Logger.getLogger(TestCashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSelectListException ex) {
            Logger.getLogger(TestCashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSubroutineException ex) {
            Logger.getLogger(TestCashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniStringException ex) {
            Logger.getLogger(TestCashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
