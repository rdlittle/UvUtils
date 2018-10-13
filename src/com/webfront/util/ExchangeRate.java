/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import asjava.uniobjects.UniSubroutine;

/**
 *
 * @author rlittle
 */
public class ExchangeRate {

    UniDynArray iList;
    UniDynArray oList;
    UniDynArray eList;
    UniSubroutine subroutine;
    UniSession session;
    
    public ExchangeRate(UniSession session) throws UniSessionException {
        this.iList = new UniDynArray();
        this.oList = new UniDynArray();
        this.eList = new UniDynArray();
        this.session = session;
        this.subroutine = session.subroutine("getUtilCurrencyRate.uvs", 3);
    }

     /**
     *
     * @param base currency code "I have"
     * @param target currency code "I want"
     * @param date internal date
     * @return
     * @throws UniException
     */

    public String getExchangeRate(String baseCountry, String targetCountry, String baseCurrency, String targetCurrency, String date, String amount) throws UniException {
        int svrStatus;
        String svrCtrlCode;
        String svrMessage;
        
        this.iList.replace(1, baseCountry);
        this.iList.replace(2, targetCountry);
        this.iList.replace(3, baseCurrency);
        this.iList.replace(4, targetCurrency);
        this.iList.replace(5, date);
        
        this.subroutine.setArg(0, this.iList);
        this.subroutine.setArg(1, this.oList);
        this.subroutine.setArg(2, this.eList);
        this.subroutine.call();
        this.oList = new UniDynArray(this.subroutine.getArg(1));
        this.eList = new UniDynArray(this.subroutine.getArg(2));
        
        svrStatus = Integer.parseInt(this.eList.extract(1).toString());
        svrCtrlCode = this.eList.extract(5).toString();
        svrMessage = this.eList.extract(2).toString();
        if (svrStatus == -1) {
            throw new UniException(svrMessage, svrStatus);
        }
        return this.oList.extract(1).toString();
    }
    
    public UniDynArray getExchangeRateInfo(String baseCountry, String targetCountry, String baseCurrency, String targetCurrency, String date, String amount) throws UniException {
        int svrStatus;
        String svrCtrlCode;
        String svrMessage;
        
        this.iList.replace(1, baseCountry);
        this.iList.replace(2, targetCountry);
        this.iList.replace(3, baseCurrency);
        this.iList.replace(4, targetCurrency);
        this.iList.replace(5, date);
        this.iList.replace(6, amount);
        
        this.subroutine.setArg(0, this.iList);
        this.subroutine.setArg(1, this.oList);
        this.subroutine.setArg(2, this.eList);
        this.subroutine.call();
        this.oList = new UniDynArray(this.subroutine.getArg(1));
        this.eList = new UniDynArray(this.subroutine.getArg(2));
        
        svrStatus = Integer.parseInt(this.eList.extract(1).toString());
        svrCtrlCode = this.eList.extract(5).toString();
        svrMessage = this.eList.extract(2).toString();
        if (svrStatus == -1) {
            throw new UniException(svrMessage, svrStatus);
        }
        return this.oList;
    }
}
