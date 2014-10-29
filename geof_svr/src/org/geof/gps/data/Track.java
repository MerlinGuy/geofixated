package org.geof.gps.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.geof.log.GLogger;
import org.geof.util.DateUtil;
import org.geof.util.FileUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class Track extends GpsDatatype {
	public static final int UNKNOWN = 0;
	public static final int FILEBASED = 0, DEVICE = 1, INTERPOLATED = 3, OTHER = 4;

	public static String[] pointstatus = { "No Points", "Points Loaded" };

	public static final String TRACKPOINT = "trackpoint";
	public static final int CSV = 1;
	public static final int GPX = 2;
	public static final int HTML = 3;
	public static final int KML = 4;
	public static final int KMZ = 5;
	public static final int NMEA = 6;
	public static final Double EarthRadiusKms = 6376.5;
	public static final double RAD = (Math.PI / 180.0);

	public static final double BADDISTANCE = 1000000.1;

	private String[] EXTENSIONS = { "unk", "csv", "gpx", "mht", "kml", "kmx", "nmea" };

	private String _name;
	private int _pointcount;
	private Extent _extent = new Extent();
	private int _createtype = OTHER;

	private double _distance = 0.0;
	private long _offset = 0;
	private boolean _interpolate = true;

	private ArrayList<GpsLocation> _tpoints = new ArrayList<GpsLocation>();
	private TimeFrame _timeframe = null;
	private double _duration = -1.0;
	private long _id = -1;

	// -------------------------------------------------------------------
	public Track() {
		_geometry = GpsDatatype.LINE;
		_pointcount = 0;
	}

	public void setId(long id) {
		_id = id;
		for (GpsLocation loc : _tpoints) {
			loc.setTrackId(id);
		}
	}
	
	public long getId(){
		return _id;
	}
	// -------------------------------------------------------------------
	public int getPointCount() {
		return _tpoints.size();
	}

	public String Name() {
		if (_name == null) {
			if ((_filename != null ) && (_filename.length() > 0)) {
				return _filename;
			} else {
				return startdate.toString() + " : " + _tpoints.size();
			}
				
		} else {
			return _name;
		}
	}
	
	
	@Override 
	public String toString() {
		return Name();
	}

	public void Name(String value) {
		_name = value;
	}

	public String Title() {
		return _filename;
	}

	public TimeFrame Timeframe() {
		if (_timeframe == null) {
			_timeframe = new TimeFrame(startdate, enddate);
		}
		return _timeframe;
	}

	public void Timeframe(TimeFrame value) {
		_timeframe = value.clone();
	}
	
	public double Distance() {
		return _distance;
	}

	public Extent getExtent() {
		return _extent;
	}

	public int PointCount() {
		return _pointcount;
	}
	
	public int CreateType() {
		return _createtype;
	}
	public Location centerPoint() {
		return (_extent == null) ? null : _extent.getCenterPoint();
	}

	public GpsLocation first() {
		return _tpoints.size() > 0 ? _tpoints.get(0) : null;
	}

	public GpsLocation last() {
		return _tpoints.size() > 0 ? _tpoints.get(_tpoints.size() - 1) : null;
	}

	public GpsLocation GetPointAtTime(TimeFrame ts) {
		long milliseconds = _timeframe.TimeSpan();
		if (ts.TimeSpan() > milliseconds) {
			return null;
		}
		GpsLocation tp = _tpoints.get((int) (_tpoints.size() * ts.TimeSpan() / milliseconds));
		return tp;
	}

	public GpsLocation GetOffsetPoint(double SecondsOffset) {
		if (SecondsOffset > _duration) {
			SecondsOffset = _duration;
		}
		double percent = (SecondsOffset / _duration);
		int indx = (int) Math.round(_pointcount * percent);
		if (indx >= _pointcount) {
			indx = _pointcount - 1;
		}
		return _tpoints.get(indx);
	}

	public double AngleOfTrackPoint(GpsLocation tpFrom) {
		int indx = tpFrom.OrderNum();
		GpsLocation tpTo;
		if (indx < _pointcount - 1) {
			tpTo = _tpoints.get(indx + 1);
		} else {
			tpTo = tpFrom;
			tpFrom = _tpoints.get(indx - 1);
		}
		return CalculateAngle(tpFrom, tpTo);
	}

	public int indexOf(GpsLocation tp) {
		return _tpoints.indexOf(tp);
	}

	public void recalcDistances() {
		_distance = 0.0;
		int count = _tpoints.size();
		for (int indx = 1; indx < count; indx++) {
			_distance += getDistance(_tpoints.get(indx), _tpoints.get(indx - 1));
			_tpoints.get(indx).Distance( getDistance(_tpoints.get(indx), _tpoints.get(indx - 1)));
		}
		if (count > 1) {
			_tpoints.get(count - 1).Angle( _tpoints.get(count - 2).Angle());
		}
	}

	public double CalculateAngle(GpsLocation tp1, GpsLocation tp2) {
		double angleRadians = Math.atan2((tp2.Latitude() - tp1.Latitude()), (tp2.Longitude() - tp1.Longitude()));
		if (angleRadians < 0) {
			angleRadians += 2 * Math.PI;
		}
		return ((angleRadians * 180) / Math.PI);
	}

	public double angleOfTravel(GpsLocation tp1) {

		double dist = 0;
		GpsLocation tp2 = null;
		int indx = tp1.OrderNum();
		boolean CanInc = true;
		boolean FlippedInc = false;
		int top = _tpoints.size() - 1;
		int count = 0;

		while ((dist < .01) && (CanInc) && (count < 10)) {
			if (indx == top) {
				FlippedInc = true;
				tp2 = _tpoints.get(top);
				indx = tp1.OrderNum();
			}
			if (CanInc) {
				if (FlippedInc) {
					tp1 = _tpoints.get(--indx);
				} else {
					tp2 = _tpoints.get(++indx);
				}
			}
			if (indx == 0) {
				CanInc = false;
			}

			dist = Math.abs(getDistance(tp1, tp2));
			count++;
		}
		if (tp2 == null)
			return 0.0;
		double angleRadians = Math.atan2((tp2.Latitude() - tp1.Latitude()), (tp2.Longitude() - tp1.Longitude()));
		if (angleRadians < 0) {
			angleRadians += 2 * Math.PI;
		}
		return ((angleRadians * 180) / Math.PI) + 35;
	}

	public String getFileExt() {
		if (_filename == null) {
			return EXTENSIONS[UNKNOWN];
		}
		return FileUtil.getExtention(_filename);
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		try {
			Extent extent = this.getExtent();
			json.put("pointcount", this.PointCount());
			json.put("minlat", extent.MinLat());
			json.put("minlon", extent.MinLon());
			json.put("maxlat", extent.MaxLat());
			json.put("maxlon", extent.MaxLon());
			json.put("startdate", DateUtil.TSDateFormat.format(this.StartDate()));
			json.put("enddate", DateUtil.TSDateFormat.format(this.EndDate()));
		} catch (JSONException e) {
			GLogger.error(e);
		}
		return json;
	}
	
	public void ClearTrackPoints() {
		_tpoints.clear();
	}

	public static String ToString(ArrayList<GpsLocation> tpoints) {
		String result = "";
		String llsep = ",";
		String pointsep = " ";
		for (GpsLocation tp : tpoints) {
			result += pointsep + tp.Latitude() + llsep + tp.Longitude();
		}
		return result.substring(1);
	}

	public void add(GpsLocation point) {
		if (_tpoints.contains(point))
			return;
		_tpoints.add(point);
		_extent.MinLat(point.Latitude());
		_extent.MaxLat(point.Latitude());
		_extent.MinLon(point.Longitude());
		_extent.MaxLon( point.Longitude());
		if ((startdate == null) || (point.Utc().getTime() < startdate.getTime())) {
			startdate = point.Utc();
			latitude = point.Latitude();
			longitude = point.Longitude();
		}
		if ((enddate == null) ||(point.Utc().getTime() > enddate.getTime())) {
			enddate = point.Utc();
		}
		point.OrderNum(_tpoints.size());;
		_pointcount = _tpoints.size();
	}

	public ArrayList<GpsLocation> TrackPoints() {
		return _tpoints;
	}

	public void TrackPoints(ArrayList<GpsLocation> value) {
		if (_tpoints != null) {
			_tpoints.clear();
			_tpoints = null;
		}
		_tpoints = value;
	}

	public String PointStatus() {
		return pointstatus[_tpoints.size() == _pointcount ? 1 : 0];
	}

	public ArrayList<GpsLocation> getTrackPoints(double distance) {
		ArrayList<GpsLocation> tpoints = new ArrayList<GpsLocation>();
		GpsLocation lastPoint = null;
		double curdistance = 0.0;
		for (GpsLocation curPoint : _tpoints) {
			if (lastPoint == null) {
				tpoints.add(curPoint);
			} else {
				curdistance += (getDistance(curPoint, lastPoint) * 1000);
				if (curdistance >= distance) {
					tpoints.add(curPoint);
					curdistance = 0.0;
				}
			}
			lastPoint = curPoint;
		}
		if ((curdistance > 0.0) && (lastPoint != null)) {
			tpoints.add(lastPoint);
		}
		return tpoints;
	}

	public ArrayList<GpsLocation> getTrackPoints(TimeFrame timespan) {
		ArrayList<GpsLocation> tpoints = new ArrayList<GpsLocation>();
		GpsLocation lastPoint = null;
		for (GpsLocation curPoint : _tpoints) {
			if ((lastPoint == null) || ((curPoint.startMillis() - lastPoint.startMillis()) >= timespan.TimeSpan())) {
				tpoints.add(curPoint);
				lastPoint = curPoint;
			}
		}
		if ((lastPoint != null) && (lastPoint != tpoints.get(tpoints.size() - 1))) {
			tpoints.add(lastPoint);
		}
		return tpoints;
	}

	public void remove(GpsLocation point) {
		if (_tpoints.contains(point)) {
			_tpoints.remove(point);
		}
	}

	// public void Geolocate(ArrayList<GpsDatatype> files) {
	// ArrayList<UFile> ufiles = new ArrayList<UFile>();
	// for (UFile ufile : ufiles) {
	// ufiles.add(ufile);
	// }
	// Geolocate(ufiles, _interpolate);
	// }
	//
	// public void Geolocate(ArrayList<UFile> files) {
	// Geolocate(files, _interpolate);
	// }
	//
	// public void Geolocate(ArrayList<UFile> files, boolean interpolate) {
	// for (UFile file : files) {
	// Geolocate(file, interpolate);
	// }
	// }
	//
	// public boolean Geolocate(UFile ufile, boolean interpolate) {
	// if (ufile.Duration.getTime() > 0) {
	// if (GeolocateTrack(ufile)) return true;
	// }
	// TrackPoint[] points = null;
	// TrackPoint newpoint = null;
	// points = getBoundingPoints(ufile.UtcDate);
	// if (points != null) {
	// newpoint = interpolateToPoint(pointsget(0), points[1], ufile.UtcDate);
	// ufile.Longitude() = newpoint.Longitude();
	// ufile.Latitude() = newpoint.Latitude();
	// }
	// return false;
	// }
	//
	// public boolean GeolocateTrack(UFile ufile) {
	// Track track = Track.interpolateTrack(this, ufile.UtcDate,
	// ufile.Duration);
	// if (track == null) {
	// return false;
	// }
	// track.name = ufile.name + "_track";
	// if ((ufile.data != null) && (ufile.FileType == UFile.VIDEO)) {
	// ((Video)ufile.data).Track = track;
	// }
	// TrackPoint tpFirst = track.TrackPointsget(0);
	// ufile.Longitude() = tpFirst.Longitude();
	// ufile.Latitude() = tpFirst.Latitude();
	// startdate = tpFirst.startdate;
	// enddate = track.TrackPoints[track.pointcount - 1].enddate;
	// return true;
	// }

	public GpsLocation[] getBoundingPoints(Date dt) {
		GpsLocation[] points = null;
		if (dt == null) {
			return points;
		}
		int count = _tpoints.size();
		for (int indx = 0; indx < (count - 1); indx++) {
			if ((dt.getTime() >= _tpoints.get(indx).Utc().getTime())
					&& (dt.getTime() <= _tpoints.get(indx + 1).Utc().getTime())) {
				points = new GpsLocation[2];
				points[0] = _tpoints.get(indx);
				points[1] = _tpoints.get(indx + 1);
				return points;
			}
		}
		return points;
	}

	public void sort() {
		if (_tpoints.size() < 2) {
			return;
		}

		Collections.sort(_tpoints);
		// renumber the points
		int ordernum = 0;
		for (GpsLocation tp : _tpoints) {
			tp.OrderNum(ordernum++);
		}

		_timeframe = new TimeFrame(first().startMillis(), last().startMillis());
	}

	public long TimeOffset() {
		return _offset;
	}

	public void TimeOffset(long value) {
		_offset = value;
	}

	public boolean Interpolate() {
		return _interpolate;
	}

	public void Interpolate(boolean value) {
		_interpolate = value;
	}

	public TimeFrame getTimeSpan() {
		if (_tpoints.size() < 2) {
			return new TimeFrame();
		}
		// sort();
		return new TimeFrame(startdate, enddate);
	}

	// -----------------------------------------------------------------
	// -----------------------------------------------------------------
	// static functions

	// / <summary>
	// / Returns the distance between two GPSpoints
	// / : Kilometers
	// / </summary>
	// / <param name="p1"></param>
	// / <param name="p2"></param>
	// / <returns></returns>
	public static double getDistance(GpsLocation p1, GpsLocation p2) {
		return getDistance(p1.Latitude(), p1.Longitude(), p2.Latitude(), p2.Longitude());
	}

	public static double getDistance(GpsLocation p1, Location loc) {
		return getDistance(p1.Latitude(), p1.Longitude(), loc.Latitude(), loc.Longitude());
	}

	public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double dLat1InRad = lat1 * RAD;
		double dLong1InRad = lon1 * RAD;
		double dLat2InRad = lat2 * RAD;
		double dLong2InRad = lon2 * RAD;
		double dLongitude = dLong2InRad - dLong1InRad;
		double dLatitude = dLat2InRad - dLat1InRad;

		// Intermediate result a.
		double a = Math.pow(Math.sin(dLatitude / 2.0), 2.0) + Math.cos(dLat1InRad) * Math.cos(dLat2InRad)
				* Math.pow(Math.sin(dLongitude / 2.0), 2.0);

		// Intermediate result c (great circle distance : Radians).
		double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
		return EarthRadiusKms * c;
	}

	public static long getTimeDiff(GpsLocation p1, GpsLocation p2) {
		return p2.getTimeDiff(p1);
	}

	public static long getTimeDiff(Date dt1, Date dt2) {
		return dt2.getTime() - dt1.getTime();
	}

	public static double getSpeed(GpsLocation p1, GpsLocation p2) {
		double millis = getTimeDiff(p1, p2);
		double distance = getDistance(p1, p2);
		return distance / millis;
	}

	public static GpsLocation interpolateToPoint(GpsLocation p1, GpsLocation p2, Date datetime) {
		try {
			GpsLocation pFirst = (p1.compareDate(p2) == -1) ? p1 : p2;
			GpsLocation pLast = (p1.compareDate(p2) == 1) ? p1 : p2;
			double timeDiff = getTimeDiff(pLast, pFirst);
			double timeOffset = getTimeDiff(pFirst.Utc(), datetime);
			if (timeOffset < 0)
				return null;
			double timePercent = timeOffset / timeDiff;
			double newLat = pFirst.Latitude() + ((pLast.Latitude() - pFirst.Latitude()) * timePercent);
			double newLon = pFirst.Longitude() + ((pLast.Longitude() - pFirst.Longitude()) * timePercent);
			double newAlt = pFirst.Altitude() + ((pLast.Altitude() - pFirst.Altitude()) * timePercent);
			return new GpsLocation(newLat, newLon,  newAlt, datetime,TimeZones.GMT(), -1);
		} catch (Exception e) {
			return null;
		}
	}

	public static GpsLocation interpolateTrackPoint(Track track, Location loc) {
		try {
			GpsLocation[] t1t2 = closestLineToLocation(track, loc);
			if ((t1t2 == null) || (t1t2[0] == null) || (t1t2[1] == null)) {
				System.out.println("Error : interpolateTrackPoint(Track track, Location loc)");
			}
			return interpolateTrackPoint(t1t2[0], t1t2[1], loc);
		} catch (Exception e) {
			return null;
		}
	}

	public static GpsLocation closestPoint(Track track, Location loc) {
		GpsLocation closestTP = null;
		double distance = 0.0;
		double shortest = 1000000000.0;
		ArrayList<GpsLocation> tpoints = track.TrackPoints();
		for (GpsLocation tp : tpoints) {
			distance = getDistance(tp, loc);
			if (distance < shortest) {
				shortest = distance;
				closestTP = tp;
			}
		}
		return closestTP;
	}

	public Object[] closestPointWithDist(Location loc) {
		GpsLocation closestTP = null;
		double distance = 0.0;
		double shortest = 1000000000.0;
		ArrayList<GpsLocation> tpoints = this.TrackPoints();
		for (GpsLocation tp : tpoints) {
			distance = getDistance(tp, loc);
			if (distance < shortest) {
				shortest = distance;
				closestTP = tp;
			}
		}
		return new Object[] { closestTP, shortest };
	}

	public static Track interpolateTrack(Track track, Date startDT, TimeFrame tracklength) {
		ArrayList<GpsLocation> tpoints = track.TrackPoints();
		int count = tpoints.size();

		// check for datetime overlap with track
		Date lastDT = TimeFrame.add(startDT, tracklength);
		Date ftime = tpoints.get(0).Utc();
		Date ltime = tpoints.get(count - 1).Utc();

		if (DateUtil.lessThan(lastDT, ftime) || DateUtil.lessThan(ltime, startDT)) {
			return null;
		}

		Date dt = new Date(startDT.getTime());
		int preIndx = 0;
		if (DateUtil.lessThan(dt, ftime)) {
			dt = new Date(ftime.getTime());
			if (DateUtil.lessThan(dt, ftime)) {
				dt = DateUtil.add(dt, 1000);
			}
		} else {
			while (DateUtil.greaterThan(dt, tpoints.get(preIndx + 1).Utc())) {
				preIndx++;
			}
		}

		// if the timespan exceeds the track move the timespan back.
		if (DateUtil.greaterThan(lastDT, ltime)) {
			lastDT = DateUtil.clone(ltime);
			if (DateUtil.greaterThan(lastDT, ltime)) {
				lastDT = DateUtil.add(lastDT, -1000);
			}
		}

		int lastIndex = count - 1;
		Track newtrack = new Track();
		newtrack._createtype = Track.INTERPOLATED;
		newtrack.Filename(track.Filename());
		newtrack._fileext = track._fileext;
		newtrack._originalname = track._originalname;
		newtrack._pointcount = newtrack.TrackPoints().size();
		GpsLocation tp;
		while ((preIndx < lastIndex) && (DateUtil.lessThan(dt, lastDT))) {
			tp = interpolateTrackPoint(tpoints.get(preIndx), tpoints.get(preIndx + 1), dt);
			newtrack.add(tp);
			dt = DateUtil.add(dt, 1000);
			while ((preIndx < lastIndex) && (DateUtil.greaterThan(dt, tpoints.get(preIndx + 1).Utc()))) {
				preIndx++;
			}
		}
		return newtrack;
	}

	public static GpsLocation interpolateTrackPoint(GpsLocation t1, GpsLocation t2, Location l3) {
		double dx31 = l3.Longitude() - t1.Longitude();
		double dx21 = t2.Longitude() - t1.Longitude();
		double dy31 = l3.Latitude() - t1.Latitude();
		double dy21 = t2.Latitude() - t1.Latitude();

		double xDelta = t2.Longitude() - t1.Longitude();
		double yDelta = t2.Latitude() - t1.Latitude();

		if ((xDelta == 0) && (yDelta == 0)) {
			// System.out.println("Error: Selection points are the same");
			return null;
		}

		double u = (dx31 * xDelta + dy31 * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		if (u < 0) {
			return t1;
		} else if (u > 1) {
			return t2;
		} else {
			double latitude = t1.Latitude() + u * dy21;
			double longitude = t1.Longitude() + u * dx21;
			double dist12 = getDistance(t1, t2);
			double dist14 = getDistance(t1.Latitude(), t1.Longitude(), latitude, longitude);
			double percent = dist14 / dist12;
			long milliseconds = (long) (t1.Utc().getTime() + percent * (t2.Utc().getTime() - t1.Utc().getTime()));
			Date newDT = new Date(milliseconds);
			double altitude = t1.Altitude() + ((t2.Altitude() - t1.Altitude()) * percent);
			return new GpsLocation(latitude, longitude, altitude, newDT, t1.Timezone(), -1);
		}
	}

	public static GpsLocation[] closestLineToLocation(Track track, Location location) {
		ArrayList<GpsLocation> tpoints = track.TrackPoints();
		int count = tpoints.size();
		double mindistance = BADDISTANCE;
		GpsLocation[] closestTPs = new GpsLocation[2];
		double distance;
		for (int indx = 1; indx < count; indx++) {
			distance = DistanceToLine(location, tpoints.get(indx - 1), tpoints.get(indx));
			if (distance < mindistance) {
				closestTPs[0] = tpoints.get(indx - 1);
				closestTPs[1] = tpoints.get(indx);
				mindistance = distance;
			}
		}
		return closestTPs;
	}

	public double distanceToLocation(Location location) {
		int count = _tpoints.size();
		double mindistance = BADDISTANCE;
		double distance;
		for (int indx = 1; indx < count; indx++) {
			distance = DistanceToLine(location, _tpoints.get(indx - 1), _tpoints.get(indx));
			if (distance < mindistance) {
				mindistance = distance;
			}
		}
		return mindistance;
	}

	public static double DistanceToLine(Location l3, GpsLocation t1, GpsLocation t2) {

		double dx31 = l3.Longitude() - t1.Longitude();
		double dx21 = t2.Longitude() - t1.Longitude();
		double dy31 = l3.Latitude() - t1.Latitude();
		double dy21 = t2.Latitude() - t1.Latitude();

		double xDelta = t2.Longitude() - t1.Longitude();
		double yDelta = t2.Latitude() - t1.Latitude();

		if ((xDelta == 0) && (yDelta == 0)) {
			// System.out.println("Error: Selection points are the same");
			return BADDISTANCE;
		}
		double u = (dx31 * xDelta + dy31 * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		if ((u < 0) || (u > 1)) {
			return BADDISTANCE;
		} else {
			double latitude = t1.Latitude() + u * dy21;
			double longitude = t1.Longitude() + u * dx21;
			return getDistance(latitude, longitude, l3.Latitude(), l3.Longitude());
		}
	}

	public static GpsLocation interpolateTrackPoint(GpsLocation t1, GpsLocation t2, Date dt) {
		double percent = ((double) (dt.getTime() - t1.Utc().getTime()))
				/ ((double) (t1.Utc().getTime() - dt.getTime()));
		double latitude = t1.Latitude() + ((t2.Latitude() - t1.Latitude()) * percent);
		double longitude = t1.Longitude() + ((t2.Longitude() - t1.Longitude()) * percent);
		double altitude = t1.Altitude() + ((t2.Altitude() - t1.Altitude()) * percent);
		return new GpsLocation(latitude, longitude, altitude, dt, t1.Timezone(), -1);
	}

	public static Track generateTrack(Track origTrack, Location syncLoc, TimeFrame startOffset, TimeFrame tracklength) {

		// Assuming that points are 1 second apart and all points are
		// interpolated to each offset.
		GpsLocation syncPoint = interpolateTrackPoint(origTrack, syncLoc);
		return generateTrack(origTrack, syncPoint, startOffset, tracklength);
	}

	public static Track generateTrack(Track origTrack, GpsLocation syncPoint, TimeFrame startOffset,
			TimeFrame tracklength) {

		// Assuming that points are 1 second apart and all points are
		// interpolated to each offset.
		Date startDT = TimeFrame.subtract(syncPoint.Utc(), startOffset);
		Track track = interpolateTrack(origTrack, startDT, tracklength);
		track.Filename(origTrack.Filename());
		track._fileext = origTrack._fileext;
		return track;
	}

}
