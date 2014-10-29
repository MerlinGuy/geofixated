package org.geof.util;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geof.db.DBInteract;
import org.geof.gps.data.GpsLocation;
import org.geof.gps.data.Track;
import org.geof.gps.image.GpsExtractor;
import org.geof.gps.track.file.TrackReader;
import org.geof.log.GLogger;
import org.geof.request.GenericRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class GeoUtil {

	public static String[] GEO_TYPES = { "multi","point", "line", "polyline"};
	public static int MULTI = 0, POINT = 1, LINE = 2, POLYLINE = 3;

	public static final String LONGITUDE = "longitude";
	public static final String LATITUDE = "latitude";
	public static final String UTCDATE = "utcdate";
	public static final String ALTITUDE = "altitude";
	public static final String AZIMUTH = "azimuth";
	
	public static final String[] REQUIRED = {LONGITUDE,LATITUDE,UTCDATE};
	
	public static final Date MINIMUM_DATE = new Date(Long.MIN_VALUE);
	
    private static final Map<String, Object> GEO_JSON = createMap();

    private static Map<String, Object> createMap() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(LONGITUDE, 0.0);
        result.put(LATITUDE, 0.0);
        result.put(UTCDATE, DateUtil.TSDateFormat.format(MINIMUM_DATE));
        result.put(ALTITUDE, 0.0);
        result.put(AZIMUTH, 0.0);
        return Collections.unmodifiableMap(result);
    }
    
    public Object getDefault(String key) {
    	if (GEO_JSON.containsKey(key)) {
    		return GEO_JSON.get(key);
    	} else {
    		return null;
    	}
    }
    
	public static JSONObject copyTo(JSONObject src, JSONObject tar, boolean override) {
		try {
			if (tar == null) {
				tar = new JSONObject();
			}

			Iterator<String> keys = GEO_JSON.keySet().iterator();
			while (keys.hasNext()) {
				String key = (String)keys.next();
				if ( ! tar.has(key) || override ) {
					if ( src == null || (! src.has(key))) {
						tar.put(key, GEO_JSON.get(key) );
					} else {
						tar.put(key, src.get(key) );
					}
				}
			}
		} catch (Exception e){
			GLogger.error(e);
		}
		return tar;
	}
	
	public static boolean validGPS(JSONObject json) {
		if (json == null) {
			return false;
		}
		for (String key : REQUIRED) {
			if (! json.has(key)) {
				return false;
			}
		}
		try {
			double latitude = json.getDouble(LATITUDE);
			double longitude = json.getDouble(LONGITUDE);
//			String strUTC = json.getString(UTCDATE);
//			Date utc = DateUtil.TSDateFormat.parse(strUTC);
			return ((latitude >= -90.0) && (latitude <= 90.0) && (longitude >= -180.0) && (longitude <= 180.0));
		} catch (Exception e) {
			return false;
		}
		
	}
	
	//TODO: there is probably a better place for this code.
	public static JSONObject saveGeometryToDb(int mediatype, File file, String ext, DBInteract dbi) {
		try {
			if (mediatype == FileUtil.PHOTO) {
				JSONObject jGeo = GpsExtractor.extractGps(file);
				JSONObject data = JsonUtil.getDataFields(jGeo);
				if (jGeo != null) {
					JSONObject keys = GenericRequest.create("point", data, dbi);
					if (keys != null) {
						int geomid = keys.getInt("pointid");
						return getJGeometry(geomid, POINT, jGeo.getString("utcdate"));
					}
				}
				
			} else if  (mediatype == FileUtil.TRACK) {
				TrackReader tr = TrackReader.resolveReader(ext);
				boolean rtn = tr.readTrack(file);
				if ( rtn ) {
					for (Track track : tr.getTracks()) {
						JSONObject data = JsonUtil.getDataFields(track.toJson());
						JSONObject keys = GenericRequest.create("line", data, dbi);
						if (keys != null) {
							int geomid = keys.getInt("lineid");
							track.setId(geomid);
							for (GpsLocation gl : track.TrackPoints()) {
								data = JsonUtil.getDataFields(gl.toJson());
								GenericRequest.create("linepoint", data, dbi);
							}
							return getJGeometry(geomid, LINE, DateUtil.tsformat(track.StartDate()));
						}
					}
				}
				return getJGeometry(null,LINE,null);
			}
		} catch (Exception e) {
			GLogger.error(e);
		}
		return getJGeometry(null,null,null);
	}
	
	public static JSONObject getJGeometry(Integer geomid, Integer geomtype, String utcdate) {
		JSONObject geom = new JSONObject();
		try {
			geom.put("geomid", geomid == null ? -1 : geomid);
			geom.put("geomtype", geomtype == null ? -1 : geomtype);
			geom.put("utcdate", utcdate == null ? DateUtil.nowStr() : utcdate);
		} catch (JSONException e) {
			GLogger.error(e);
		}
		return geom;
	}

}
