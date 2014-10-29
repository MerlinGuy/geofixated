package org.geof.gps.data;

import java.util.Date;

public class Location {

	protected Double _latitude = 0.0;
	protected Double _longitude = 0.0;
	protected Double _altitude = 0.0;
	protected Date _utc;
	protected TimeZone _timezone = null;

	public Location() {
	}

	public Location(Double latitude, Double longitude) {
		this(latitude, longitude, 0.0, null, null);
	}

	public Location(Double latitude, Double longitude, Double altitude) {
		this(latitude, longitude, altitude, null, null);
	}

	public Location(Double latitude, Double longitude, Double altitude, Date utc) {
		this(latitude, longitude, altitude, utc, null);
	}

	public Location(Double latitude, Double longitude, Double altitude, Date utc, TimeZone timezone) {
		_latitude = latitude;
		_longitude = longitude;
		_altitude = altitude;
		_utc = utc;
		_timezone = timezone;
	}

	public Double Latitude() {
		return _latitude;
	}

	public void Latitude(Double value) {
		_latitude = value;
	}

	public Double Longitude() {
		return _longitude;
	}

	public void Longitude(Double value) {
		_longitude = value;
	}

	public Double Altitude() {
		return _altitude;
	}

	public void Altitude(Double altitude) {
		_altitude = altitude;
	}

	public Date Utc() {
		return _utc;
	}

	public void Utc(Date utc) {
		_utc = utc;
	}

	public long startMillis() {
		return (_utc == null) ? 0 : _utc.getTime();
	}

	public TimeZone Timezone() {
		return _timezone;
	}

	public void Timezone(TimeZone timezone) {
		_timezone = timezone;
	}

	public boolean isValid() {
		return ((_latitude != null) && (_longitude != null) && (_latitude >= -90.0) && (_latitude <= 90.0)
				&& (_longitude >= -180.0) && (_longitude <= 180.0));
	}

	public static boolean isValid(Double latitude, Double longitude) {
		return ((latitude >= -90.0) && (latitude <= 90.0) && (longitude >= -180.0) && (longitude <= 180.0));
	}

	public boolean setLatLon(String sLatitude, String sLongitude) {
		try {
			_latitude = Double.parseDouble(sLatitude);
		} catch (Exception e) {
			_latitude = null;
		}
		try {
			_longitude = Double.parseDouble(sLongitude);
		} catch (Exception e) {
			_longitude = null;
		}
		return isValid();
	}

	public static Location parse(String sLatitude, String sLongitude) {
		Double latitude, longitude;
		try {
			latitude = Double.parseDouble(sLatitude);
		} catch (Exception e) {
			latitude = null;
		}
		try {
			longitude = Double.parseDouble(sLongitude);
		} catch (Exception e) {
			longitude = null;
		}
		if (isValid(latitude, longitude)) {
			return new Location(latitude, longitude);
		} else {
			return null;
		}
	}
}
