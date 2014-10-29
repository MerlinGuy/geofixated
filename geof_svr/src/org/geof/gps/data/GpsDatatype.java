package org.geof.gps.data;

import java.util.Date;


public class GpsDatatype {

    public static final int OFFLINE = 0, ONLINE = 1;
    public static final int POINT = 0, LINE = 1, POLYGON = 2;
    public static final int MOMENT = 0, TIMEFRAME = 1;
    public static TimeFrame NoGmtOffset = new TimeFrame();

    public static final int UNKOWN = 0, FILE = 1, SERVER = 2;

    protected Date startdate;
    protected Date enddate;
    protected double latitude = -999;
    protected double longitude = -999;
    protected TimeZone timezone = TimeZones.GMT();

    protected int source = 0;

    protected String _filename = "";
    protected String _fileext = "";
    protected String _originalname = "";
    protected String _checksumval = "";
    protected String _notes = "";
    protected long _storageid = 0;
    protected long _filesize = 0;

    protected String _sendname = "";
    protected int _geometry = -1;
    protected int _temporaltype = -1;

    //----------------------------------------------------------------
    
    public String Filename() {
    	return _filename;
    }
    
    public void Filename(String value) {
    	_filename = value;
    }

    public int Geometry() {
        return _geometry; 
    }
    public void Geometry(int value) { 
    	_geometry = value; 
    }

    public int TemporalType() {
        return _temporaltype;
    }

    public Date StartDate() {
        return startdate;
    }
    
    public void StartDate(Date value) {
            startdate = value;
    }
    
    public long startMillis() {
    	return startdate.getTime();
    }

    public Date EndDate() {
            return enddate;
        }
    public void EndDate(Date value) {
            enddate = value;
    }

    public long endMillis() {
    	return enddate.getTime();
    }

    public Date UtcDate() {
            return timezone.GetLocalAsGMT(startdate);
    }
    public void UtcDate(Date value) {
            startdate = timezone.GetGMTAsLocal(new Date(value.getTime()));
    }

    public TimeFrame Duration() {
         return (HasDuration()) ? new TimeFrame(startdate, enddate) : new TimeFrame();
    }

    public double Latitude() {
        return latitude; 
    }
    public void Latitude(double value) {
           latitude = value;
    }

    public double Longitude () {
        return longitude; 
    }
        public void Longitude (double value) {
        	longitude = value;
    }
        
    public Location getLocation() {
    	return new Location(latitude, longitude);
    }

    public boolean HasLatLon() {
        return ((latitude >= -90.0) && (latitude <= 90.0) && (longitude >= -180.0) && (longitude <= 180.0));
    }

    public boolean HasUtcDate() {
        return (startdate != null);
    }

    public boolean HasGPS() {
        return (HasLatLon() && HasUtcDate());
    }

    public boolean HasDuration() {
        return ((startdate != null) && (enddate != null)); 
    }

    public TimeZone Timezone() {
         return timezone;
        }
    public void Timezone(TimeZone value) {
            timezone = value;
    }

    public String TimeZoneAbrev() {
        return timezone.Abrev;
    }

    public boolean IsDST() {
        return timezone.UseDST(); 
    }

    public Extent Extent() {
    	return HasLatLon() ? new Extent (latitude, longitude, latitude, longitude) : new Extent ();
    }
    
    
    

}