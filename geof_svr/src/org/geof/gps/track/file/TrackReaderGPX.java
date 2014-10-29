package org.geof.gps.track.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.geof.gps.data.GpsLocation;
import org.geof.gps.data.TimeZones;
import org.geof.gps.data.Track;
import org.geof.util.XMLReader;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class TrackReaderGPX extends TrackReader {

	public final static String GPX = "gpx";
	public final static String NAME = "name";
	public final static String TIME = "time";
	public final static String BOUNDS = "bounds";
	public final static String MINLAT = "minlat";
	public final static String MINLON = "minlon";
	public final static String MAXLAT = "maxlat";
	public final static String MAXLON = "maxlon";
	public final static String WPT = "wpt";
	public final static String LAT = "lat";
	public final static String LON = "lon";
	public final static String ELE = "ele";
	public final static String RTE = "rte";
	public final static String RTEPT = "rtept";

	protected XMLReader _reader = null;

	public TrackReaderGPX() {
	}

	@Override
	public boolean readTrack(File file) {
		if (!file.exists()) {
			return false;
		}
		_file = file;
		_reader = XMLReader.getXMLReader(file);
		return readTrack();
	}

	/*
	 * <gpx version="1.0" creator="ExpertGPS 1.1 - http://www.topografix.com"
	 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 * xmlns="http://www.topografix.com/GPX/1/0" xsi:schemaLocation=
	 * "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd"
	 * >
	 * 
	 * <time>2002-02-27T17:18:33Z</time> <bounds minlat="42.401051"
	 * minlon="-71.126602" maxlat="42.468655" maxlon="-71.102973"/> <wpt
	 * lat="42.438878" lon="-71.119277"> <ele>44.586548</ele>
	 * <time>2001-11-28T21:05:28Z</time> <name>5066</name>
	 * <desc><![CDATA[5066]]></desc> <sym>Crossing</sym>
	 * <type><![CDATA[Crossing]]></type> </wpt> ... <rte> <rtept lat="42.431240"
	 * lon="-71.109236"> <ele>26.561890</ele> <time>2001-11-07T23:53:41Z</time>
	 * <name>GATE6</name> <desc><![CDATA[Gate6]]></desc> <sym>Trailhead</sym>
	 * <type><![CDATA[Trail Head]]></type> </rtept> .... </rte>
	 */

	public boolean readTrack() {
		boolean rtn = false;
		try {
			//Extent extent = parseExtent(_reader.getFirstElement(BOUNDS));
			NodeList nl = _reader.getNodeList(WPT);
			if ((nl != null) && (nl.getLength() > 0)) {
				Track wptTrack = createTrack(nl);
				_tracks.add(wptTrack);
			}

			NodeList nlRTE = _reader.getNodeList(RTE);
			if ((nlRTE != null) && (nlRTE.getLength() > 0)) {
				int count = nlRTE.getLength();
				Track route;
				Element elRTE;
				for (int indx = 0; indx < count; indx++) {
					elRTE = (Element) nlRTE.item(indx);
					nl = _reader.getNodeList(elRTE, RTEPT);
					route = createTrack(nl);
					if (route != null) {
						_tracks.add(route);
					}
				}
			}

			rtn = true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		resort();
		return rtn;
	}

	private Track createTrack(NodeList nl) {
		/*
		 * <ele>44.586548</ele> <time>2001-11-28T21:05:28Z</time>
		 * <name>5066</name> <desc><![CDATA[5066]]></desc> <sym>Crossing</sym>
		 * <type><![CDATA[Crossing]]></type>
		 */
		if (nl == null) {
			return null;
		}
		int count = nl.getLength();

		Element eTrackpoint;
		Double latitude;
		Double longitude;
		double altitude;
		Date tpDate;
		GpsLocation tp;
		Track track = new Track();
		int ordernum = 0;
		for (int indx = 0; indx < count; indx++) {
			eTrackpoint = (Element) nl.item(indx);

			latitude = parseDouble(eTrackpoint.getAttribute(LAT));
			longitude = parseDouble(eTrackpoint.getAttribute(LON));
			Double dblAltitude = parseDouble(_reader.getFirstElementText(eTrackpoint, ELE));
			altitude = dblAltitude == null ? 0.0 : dblAltitude.doubleValue();
			tpDate = parseDate(_reader.getFirstElementText(eTrackpoint, TIME));
			if ((latitude != null) && (longitude != null) && (tpDate != null)) {
				tp = new GpsLocation(latitude, longitude, altitude, tpDate, TimeZones.GMT(), ordernum++);
				track.add(tp);
			}
		}
		return track;
	}

	private Double parseDouble(String value) {
		if ((value != null) && (value.length() > 0)) {
			try {
				return new Double(value);
			} catch (NumberFormatException e) {
			}
		}
		return null;
	}

	private static Date parseDate(String strDate) {
		if (strDate == null) {
			return null;
		}
		try {
			// 2011-07-13T14:15:17Z
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			return format.parse(strDate);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

//	private static Extent parseExtent(Element bounds) {
//		if (bounds == null)
//			return null;
//		// <bounds minlat="42.401051" minlon="-71.126602" maxlat="42.468655"
//		// maxlon="-71.102973"/>
//		double minlat = Double.parseDouble(bounds.getAttribute(MINLAT));
//		double minlon = Double.parseDouble(bounds.getAttribute(MINLON));
//		double maxlat = Double.parseDouble(bounds.getAttribute(MAXLAT));
//		double maxlon = Double.parseDouble(bounds.getAttribute(MAXLON));
//		return new Extent(minlat, minlon, maxlat, maxlon);
//
//	}

}