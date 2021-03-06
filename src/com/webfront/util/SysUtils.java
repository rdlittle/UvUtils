/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.util;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniStringException;
import asjava.uniobjects.UniCommand;
import asjava.uniobjects.UniCommandException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.NumberFormat;

/**
 *
 * @author rlittle
 */
public class SysUtils {
    
    public static int sum(UniDynArray uda) {
        int result = 0;
        int values = uda.dcount(1);
        for (int value = 1; value <= values; value++) {
            String val = uda.extract(1, value).toString();
            if (val == null || val.isEmpty()) {
                continue;
            }
            int v = Integer.parseInt(val);
            result += v;
        }
        return result;
    }

    public static int locate(String target, UniDynArray uda) {
        int attrs = uda.dcount();
        for (int attr = 1; attr <= attrs; attr++) {
            String value = uda.extract(attr, 1).toString();
            if (value.equals(target)) {
                return attr;
            }
        }
        return -1;
    }

    public static Result locate(String target, UniDynArray uda, int attr) {
        Result r = new Result();
        int loc = -1;
        int vals = uda.dcount(attr);
        if(uda.extract(attr,1).toString().isEmpty()) {
            vals=0;
        }
        for (int val = 1; val <= vals; val++) {
            String value = uda.extract(attr, val).toString();
            if (value.equals(target)) {
                loc = val;
                r.isSuccess=true;
                r.location=loc;
                break;
            }
        }

        if (loc == -1) {
            for (int val = vals; val > 0; val--) {
                String value = uda.extract(attr, val).toString();
                int pos = target.compareToIgnoreCase(value);
                if (pos >= 0) {
                    r.location = val+1;
                    break;
                }
            }
        }
        if(vals>0 && r.location==-1) {
            r.location=1;
        }
        return r;
    }
    
    public static String getIdate(UniSession session, String oDate) {
        try {
            String args = "ODATE";
            if (oDate != "") {
                args += " "+oDate;
            }
            UniCommand cmd = session.command(args);
            cmd.exec();
            String response = cmd.response();
            return response.split(" ")[0];
        } catch (UniCommandException ex) {
            Logger.getLogger(SysUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniSessionException ex) {
            Logger.getLogger(SysUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    public static String getOdate(UniSession session, String iDate) {
        try {
            String args = "ODATE";
            if (iDate != "") {
                args += " "+iDate;
            }            
            UniCommand cmd = session.command(args);
            cmd.exec();
            String response = cmd.response();
            return response.split(" ")[0];
        } catch (UniSessionException ex) {
            Logger.getLogger(SysUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UniCommandException ex) {
            Logger.getLogger(SysUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    } 
    
    public static String asString(int value) {
        StringBuilder sb = new StringBuilder();
        sb.append((char)value);
        return sb.toString();
    }
    
    public static UniDynArray oconvs(UniSession session, UniDynArray uda, String convCode) {
        int vals = uda.dcount(1);
        for (int val = 1; val <= vals; val++) {
            String value = uda.extract(1, val).toString();
            try {
               value = session.oconv(value, convCode).toString();
               uda.replace(1, val, value);
            } catch (UniStringException ex) {
                Logger.getLogger(SysUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return uda;
    }
    
    public static UniDynArray iconvs(UniSession session, UniDynArray uda, String convCode) {
        int vals = uda.dcount(1);
        for (int val = 1; val <= vals; val++) {
            String value = uda.extract(1, val).toString();
            try {
               value = session.iconv(value, convCode).toString();
               uda.replace(1, val, value);
            } catch (UniStringException ex) {
                Logger.getLogger(SysUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return uda;
    }
    
    public static UniDynArray muls(UniDynArray uda) {
        int vals = uda.dcount(1);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        for (int val = 1; val <= vals; val++) {
            String v = uda.extract(1, val).toString();
            String m = uda.extract(2, val).toString();
            if (v.isEmpty() || m.isEmpty()) {
                continue;
            }
            boolean hasDecimal = v.indexOf(".") > 0;
            Double value = Double.parseDouble(v);
            Double mult = Double.parseDouble(m);
            if (!hasDecimal) {
                value /= 100;
            }
            if(m.indexOf(".") == -1) {
                mult /= 100;
            }
            Double p = value * mult;
            String result = nf.format(p);
            while(result.startsWith("0")) {
                result = result.replaceFirst("0", "");
            }
            if (!hasDecimal) {
                result = result.replace(".", "");
            }
            uda.replace(1, val, result);
        }
        return uda.extract(1);
    }
    
}
