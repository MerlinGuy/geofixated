package org.geof.gps.image;

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;

import org.geof.log.GLogger;
import org.geof.util.DateUtil;
import org.json.JSONObject;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

public class GpsExtractor {

	public static JSONObject extractGps(File jpegFile) {
		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(jpegFile);
			GpsDirectory directory = metadata.getDirectory(GpsDirectory.class);
			GeoLocation geoloc = directory.getGeoLocation();
			if (geoloc == null) {
				return null;
			}
			String strDate = (String) directory.getObject(GpsDirectory.TAG_GPS_DATE_STAMP);
			Rational[] raTime  = directory.getRationalArray(GpsDirectory.TAG_GPS_TIME_STAMP);
//			Object obj = directory.getObject(GpsDirectory.TAG_GPS_ALTITUDE_REF);
			double altitude = directory.getRational(GpsDirectory.TAG_GPS_ALTITUDE).doubleValue();

			JSONObject json = new JSONObject();
			json.put("longitude", geoloc.getLongitude());
			json.put("latitude", geoloc.getLatitude());
			json.put("utcdate", convertTSDateFormat(strDate, raTime));
			json.put("altitude", altitude);
			json.put("azimuth", 0.0);
			return json;
			
		} catch (Exception e) {
			GLogger.error(e);
		}
		return null;
	}
	
	public static Date convertToDate(String date, Rational[] time) throws Exception {
		String strDate = convertTSDateFormat(date, time);
		return DateUtil.parse(strDate,true);
	}
	public static String convertTSDateFormat(String date, Rational[] time) {
		StringTokenizer st = new StringTokenizer(date, ":");
		String strDate = st.nextToken() + "-" 
				+ st.nextToken() + "-" 
				+ st.nextToken() + " "
				+ time[0].intValue() + ":"
				+ time[1].intValue() + ":"
				+ (int) (time[2].doubleValue() * 10);
		return strDate;
	}
}
