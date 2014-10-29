package org.geof.gps.track.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.geof.gps.data.GpsLocation;
import org.geof.gps.data.TimeFrame;
import org.geof.gps.data.TimeZones;
import org.geof.gps.data.Track;
import org.geof.log.GLogger;
import org.geof.util.XMLReader;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TrackReaderKML extends TrackReader {

	public final static String FOLDER = "Folder";
	public final static String NAME = "name";
	public final static String WAYPOINTS = "WayPoints";
	public final static String POINT = "Point";
	public final static String COORDINATES = "coordinates";
	public final static String PLACEMARK = "Placemark";
	public final static String TIMESPAN = "TimeSpan";
	public final static String BEGIN = "begin";
	public final static String END = "end";
	public final static String LINESTRING = "LineString";

	protected XMLReader _reader = null;

	public TrackReaderKML() {
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

	public boolean readTrack() {
		boolean rtn = false;
		try {
			NodeList nl;
			nl = _reader.getNodeList(FOLDER);
			if ((nl == null) || (nl.getLength() == 0)) {
				return false;
			}
			Element elMainFolder = (Element) nl.item(0);

			_curTrackName = _reader.getFirstElementText(elMainFolder, NAME);
			NodeList nlFolders = _reader.getNodeList(elMainFolder, FOLDER);
			for (int indx =0; indx < nlFolders.getLength(); indx++) {
				readSubTrack(nlFolders.item(indx));
			}

			rtn = true;
		} catch (Exception e) {
			GLogger.error(e);
		}
		resort();
		return rtn;
	}

	private boolean readSubTrack(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			TimeFrame timeframe = null;
			
			Element enode = (Element) node;
			String trackName = _reader.getFirstElementText(enode, NAME);
			Element timespan = _reader.getFirstElement(enode, TIMESPAN);
			if (timespan != null) {
				timeframe = parseTimeframe(timespan);
			}
			Element placeMark = _reader.getFirstElement(enode, PLACEMARK);
			if (placeMark == null) {
				return false;
			}
			Element lineString = _reader.getFirstElement(placeMark, LINESTRING);
			if (lineString == null) {
				return false;
			}
			Element coordinates = _reader.getFirstElement(placeMark, COORDINATES);
			if (coordinates == null) {
				return false;
			}
			Track track = parseTrackFromCoordinates(coordinates,timeframe);
			if (track != null) {
				track.Name(trackName);
				this._tracks.add(track);
			}
			return true;
		}
		return false;
	}
	
	private Track parseTrackFromCoordinates(Element coordinates, TimeFrame timeframe) {
		String strCoord = coordinates.getTextContent();
		//-105.0269416,40.561151833333334,1501.2
		StringTokenizer st = new StringTokenizer(strCoord," ");
		int trackpointCount = st.countTokens();
		int ordernum = 0;
		Date[] utcdate = null;
		if (timeframe != null) {
			utcdate = timeframe.calcPointsInTime(trackpointCount, true);
		}
		Track track = new Track();
		while (st.hasMoreTokens()) {
			StringTokenizer stTP = new StringTokenizer(st.nextToken(),",");
			double latitude = Double.parseDouble(stTP.nextToken());
			double longitude = Double.parseDouble(stTP.nextToken());
			double altitude =  Double.parseDouble(stTP.nextToken());
			Date tpDate = ((utcdate == null) ? null : utcdate[ordernum]);
			GpsLocation tp = new GpsLocation(latitude, longitude, altitude, tpDate, TimeZones.GMT(), ordernum++);
			track.add( tp );
		}
		return track;
	}
	
	private TimeFrame parseTimeframe(Element element) {
		String startStr = _reader.getFirstElementText(element, BEGIN);
		String endStr = _reader.getFirstElementText(element, END);
		Date startdate = parseDate(startStr);
		Date enddate = parseDate(endStr);
		TimeFrame tf = new TimeFrame(startdate, enddate);
		return tf;
	}

	private static Date parseDate(String strDate) {
		try {
			//2011-07-13T14:15:17Z
			 SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			 return format.parse(strDate);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

}