/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniException;
import asjava.uniclientlibs.UniStringException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;
import asjava.uniobjects.UniSubroutineException;
import com.webfront.app.CashbackCompare;
import com.webfront.u2.model.UvData;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rlittle
 */
public class CalcIbvCashback {

    private UniSession session;
    private ExchangeRate rate;
    int svrStatus;
    String svrCtrlCode;
    String svrMessage;
    private UniDynArray cashback;
    private UniDynArray ibv;
    private boolean isCashbackCountry;
    private boolean isIbvCountry;
    private boolean isCipCountry;

    public CalcIbvCashback(UniSession sess) throws UniSessionException {
        this.session = sess;
        this.rate = new ExchangeRate(this.session);
        cashback = new UniDynArray();
        ibv = new UniDynArray();
    }

    public void calc(UvData aoRec) throws UniSessionException, UniSubroutineException, UniStringException {
        setEligibility(aoRec);
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

        UniSubroutine subroutine = session.subroutine("getCashbackCalc.uvs", 3);
        subroutine.setArg(0, iList);
        subroutine.setArg(1, oList);
        subroutine.setArg(2, eList);
        subroutine.call();
        eList = subroutine.getArgDynArray(2);
        svrStatus = Integer.parseInt(eList.extract(1).toString());
        svrCtrlCode = eList.extract(5).toString();
        svrMessage = eList.extract(2).toString();
        if (svrStatus == -1) {
            try {
                throw new UniException("[" + svrCtrlCode + "] " + svrMessage, 0);
            } catch (UniException ex) {
                Logger.getLogger(CalcIbvCashback.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        oList = subroutine.getArgDynArray(1);
        cashback = oList.extract(1);
        ibv = oList.extract(5);
        applyExchangeRates(aoRec);
    }

    public void applyExchangeRates(UvData aoRec) throws UniStringException {
        String hcc = aoRec.getData().extract(290).toString();
        String reportingCurrency = aoRec.getData().extract(368).toString();
        String iDate = aoRec.getData().extract(161).toString();
        cashback = SysUtils.oconvs(session, cashback, "mr2");
        ibv = SysUtils.oconvs(session, ibv, "mr2");
        
        try {
            String exchRate = "0";
            String ibvExchRate = "0";
            if (isCashbackCountry) {
                exchRate = rate.getExchangeRate("", hcc, reportingCurrency, "", iDate, "");
            }
            if(isIbvCountry) {
                ibvExchRate = rate.getExchangeRate("", "", reportingCurrency, "USD", iDate, "");
            }
            if (isCipCountry) {
                exchRate = ibvExchRate;
            }
            int vals = cashback.dcount(1);
            for (int val = 1; val <= vals; val++) {
                cashback.replace(2, val, exchRate);
            }
            vals = ibv.dcount(1);
            for (int val = 1; val <= vals; val++) {
                ibv.replace(2, val, ibvExchRate);
            }
            cashback = SysUtils.muls(cashback).extract(1);
            ibv = SysUtils.muls(ibv).extract(1);
            if(isCipCountry) {
                cashback = new UniDynArray(ibv);
            }
            cashback = SysUtils.iconvs(session, cashback, "mr22");
            ibv = SysUtils.iconvs(session, ibv, "mr22");
        } catch (UniException ex) {
            Logger.getLogger(CashbackCompare.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getCashbackTotal() {
        return Integer.toString(SysUtils.sum(cashback));
    }

    public String getIbvTotal() {
        return Integer.toString(SysUtils.sum(ibv));
    }

    private void setEligibility(UvData aoRec) throws UniSessionException, UniSubroutineException {
        UniSubroutine sub = this.session.subroutine("GET.CTRY.IBV.CB.INFO", 3);
        UniDynArray iList = new UniDynArray();
        UniDynArray oList = new UniDynArray();
        UniDynArray eList = new UniDynArray();
        
        iList.replace(1, aoRec.getData().extract(290).toString());
        iList.replace(2, aoRec.getData().extract(161,1).toString());
        sub.setArg(0, iList);
        sub.setArg(1, oList);
        sub.setArg(2, eList);
        
        sub.call();
        eList = sub.getArgDynArray(2);
        svrStatus = Integer.parseInt(eList.extract(1).toString());
        svrCtrlCode = eList.extract(5).toString();
        svrMessage = eList.extract(2).toString();
        if (svrStatus == -1) {
            try {
                throw new UniException("[" + svrCtrlCode + "] " + svrMessage, 0);
            } catch (UniException ex) {
                Logger.getLogger(CalcIbvCashback.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        oList = sub.getArgDynArray(1);
        isIbvCountry = oList.extract(1).toString().equals("0");
        isCashbackCountry = oList.extract(2).toString().equals("0");
        isCipCountry = oList.extract(7).toString().equals("1");
    }

}
