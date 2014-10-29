package org.geof.gps.data;

import java.util.ArrayList;

public class TimeZones {
    private static ArrayList<TimeZone> _timezones = new ArrayList<TimeZone>();
    private static boolean _initialized = false;

    private static void initialize() {
        _timezones.clear();
        _timezones.add(new TimeZone("GMT", "Greenwich Mean", 0));
        _timezones.add(new TimeZone("UTC", "Universal Coordinated", 0));
        _timezones.add(new TimeZone("ECT", "European Central", -1));
        _timezones.add(new TimeZone("EET", "Eastern European", -2));
        _timezones.add(new TimeZone("ART", "Egypt Standard", -2));
        _timezones.add(new TimeZone("EAT", "Eastern African", -3));
        _timezones.add(new TimeZone("MET", "Middle East", -3.5));
        _timezones.add(new TimeZone("NET", "Near East", -4));
        _timezones.add(new TimeZone("PLT", "Pakistan Lahore", -5));
        _timezones.add(new TimeZone("IST", "India Standard", -5.5));
        _timezones.add(new TimeZone("BST", "Bangladesh Standard", -6));
        _timezones.add(new TimeZone("VST", "Vietnam Standard", -7));
        _timezones.add(new TimeZone("CTT", "China Taiwan", -8));
        _timezones.add(new TimeZone("JST", "Japan Standard", -9));
        _timezones.add(new TimeZone("ACT", "Australia Central", -9.5));
        _timezones.add(new TimeZone("AET", "Australia Eastern", -10));
        _timezones.add(new TimeZone("SST", "Solomon Standard", -11));
        _timezones.add(new TimeZone("NST", "New Zealand Standard", -12));
        _timezones.add(new TimeZone("MIT", "Midway Islands", 11));
        _timezones.add(new TimeZone("HST", "Hawaii Standard", 10));
        _timezones.add(new TimeZone("AST", "Alaska Standard", 9));
        _timezones.add(new TimeZone("PST", "Pacific Standard", 8));
        _timezones.add(new TimeZone("MST", "Mountain Standard", 7));
        _timezones.add(new TimeZone("PNT", "Phoenix Standard", 7));
        _timezones.add(new TimeZone("CST", "Central Standard", 6));
        _timezones.add(new TimeZone("EST", "Eastern Standard", 5));
        _timezones.add(new TimeZone("IET", "Indiana Eastern Standard", 5));
        _timezones.add(new TimeZone("PRT", "Puerto Rico / Virgin Islands", 4));
        _timezones.add(new TimeZone("CNT", "Canada Newfoundland", 3.5));
        _timezones.add(new TimeZone("AGT", "Argentina Standard", 3));
        _timezones.add(new TimeZone("BET", "Brazil Eastern", 3));
        _timezones.add(new TimeZone("CAT", "Central African", 1));
        _initialized = true;
    }

    public static ArrayList<TimeZone> GetTimeZones() {
            if (!_initialized) {
                initialize();
            }
            return _timezones;
    }

    public static int GetIndexOf(String abrev) {
        for(TimeZone tz : _timezones) {
            if (tz.Abrev.toUpperCase().compareTo(abrev.toUpperCase()) == 0) {
                return _timezones.indexOf(tz);
            }
        }
        return -1;
    }

    public static TimeZone GetTimeZone(String abrev) {
        if (!_initialized) {
            initialize();
        }
        for (TimeZone tz : _timezones) {
            if (tz.Abrev.toUpperCase().compareTo(abrev.toUpperCase()) == 0) {
                return tz;
            }
        }
        return null;
    }

    public static TimeZone GetTimeZone(double TimezoneOffsetGmt) {
        if (!_initialized) {
            initialize();
        }
        for (TimeZone tz : _timezones) {
            if (tz.Adjust == TimezoneOffsetGmt) {
                return tz;
            }
        }
        return null;
    }

    public static TimeZone GMT() {
            if (!_initialized) {
                initialize();
            }
            return _timezones.get(0);
    }

//    public static Date Convert(Date dt, TimeZone FromTZ, TimeZone ToTZ) {
//        if (FromTZ.Abrev == ToTZ.Abrev) {
//            return dt;
//        }
//        double min1 = 0;
//        if (FromTZ.Abrev != "GMT") {
//            min1 = (FromTZ.Adjust * 60) + (FromTZ.UseDST() ? -60 : 0);
//        }
//        double min2 = (ToTZ.Adjust * 60) + (ToTZ.UseDST() ? -60 : 0);
//        return new Date(dt.getTime() )
//        TimeFrame tsAdjust = new TimeFrame(0, (int)(min1 - min2));
//        return dt.Add(tsAdjust);
//    }

}
