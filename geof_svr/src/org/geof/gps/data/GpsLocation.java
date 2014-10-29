package org.geof.gps.data;

import java.util.Date;

import org.geof.log.GLogger;
import org.geof.util.DateUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class GpsLocation extends Location implements Comparable<Object> {

	private int _ordernum = -1;
	private long _trackid = -1;
	private double _angle = 0.0;
	private double _distance = 0.0;

	public GpsLocation() {
	}

	public GpsLocation(double latitude, double longitude, double altitude, Date utc, TimeZone timezone, int id) {
		super(latitude, longitude, altitude, utc, timezone);
		this._ordernum = id;
	}

	public GpsLocation(double latitude, double longitude, Date utc, int ordernum) {
		super(latitude, longitude, 0.0, utc);
		this._ordernum = ordernum;
	}

	public int OrderNum() {
		return _ordernum;
	}

	public void OrderNum(int value) {
		_ordernum = value;
	}

	public long TrackId() {
		return _trackid;
	}
	
	public void setTrackId(long trackid) {
		_trackid = trackid;
	}

	public double Angle() {
		return _angle;
	}

	public void Angle(double value) {
		_angle = value;
	}

	public double Distance() {
		return _distance;
	}

	public void Distance(double value) {
		_distance = value;
	}

	public long getTimeDiff(GpsLocation tp) {
		return this._utc.getTime() - tp._utc.getTime();
	}

	public int compareDate(GpsLocation tp) {
		long td = getTimeDiff(tp);
		return td < 0 ? -1 : (td > 0 ? 1 : 0);
	}

	@Override
	public int compareTo(Object tp) {
		long td = getTimeDiff((GpsLocation) tp);
		return td < 0 ? -1 : (td > 0 ? 1 : 0);
	}
	
	public static GpsLocation parse(String sLatitude, String sLongitude) {
		GpsLocation gpsL = new GpsLocation();
		gpsL.setLatLon(sLatitude, sLongitude);
		return gpsL.isValid()? gpsL: null;
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		try {
			json.put("lineid", this.TrackId());
			json.put("ordernum", this.OrderNum());
			json.put("longitude", this.Latitude());
			json.put("latitude", this.Longitude());
			json.put("utcdate", DateUtil.TSDateFormat.format(this.Utc()));
			json.put("altitude", this.Altitude());
			json.put("azimuth", this.Angle());
			json.put("distance", this.Distance());
		} catch (JSONException e) {
			GLogger.error(e);
		}
		return json;
	}
	
	public static JSONObject toJson(double lat, double lon, Date date, double altitude, double azimuth) {
		JSONObject json = new JSONObject();
		try {
			json.put("latitude", lat);
			json.put("longitude", lon);
			json.put("utcdate", DateUtil.TSDateFormat.format(date));
			json.put("altitude", altitude);
			json.put("azimuth", azimuth);
		} catch (JSONException e) {
			GLogger.error(e);
		}
		return json;
	}
	
}