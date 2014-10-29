package org.geof.gps.data;

import java.util.Date;

public class TimeZone {
    public String Name;
    public String Abrev ;
    public Double Adjust;

    private boolean _useDst = false;

    //--------------------------------------------------------------
    public TimeZone(String abrev, String name, int timeadjust) {
        initialize(abrev, name, (double)timeadjust, false);
    }

    public TimeZone(String abrev, String name, Double timeadjust) {
        initialize(abrev, name, timeadjust, false);
    }

    public TimeZone(String abrev, String name, Double timeadjust, boolean useDST) {
        initialize(abrev, name, timeadjust, useDST);
    }
    //--------------------------------------------------------------

    private void initialize(String abrev, String name, Double timeadjust, boolean useDST) {
        Name = name;
        Abrev = abrev;
        Adjust = timeadjust;
        _useDst = useDST;
    }

    public boolean UseDST() {
        return _useDst;
    }
    
    public void UseDst(boolean value) {
            _useDst = value;
    }

    public long GmtOffset() {
        double minutes = (Adjust * 60) + (_useDst ? -60 : 0);
        return (long) (60.0 * 1000.0 * minutes);
    }

    public Date GetLocalAsGMT(Date dt) {
        return new Date (dt.getTime() + GmtOffset());
    }

    public Date GetGMTAsLocal(Date dt) {
        return new Date( dt.getTime() - GmtOffset());
    }

    public TimeZone Clone() {
        return new TimeZone(Abrev, Name, Adjust, _useDst); 
    }

    public long getAdjustAsMillis() {
    	return (long) (60.0 * 1000.0 * Adjust);
    }
}
