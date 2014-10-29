package org.geof.gps.track.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.geof.gps.data.Extent;
import org.geof.gps.data.GpsDatatype;
import org.geof.gps.data.Track;
import org.geof.gps.data.GpsLocation;
import org.geof.util.FileUtil;

public abstract class TrackReader {

	public final static String TRACK_READER_ROOT = "org.geof.gps.track.file"; 
	
	protected static HashMap<String, String> _fileExtLookup = null;
    protected ArrayList<Track> _tracks = new ArrayList<Track>();
    protected Track _curTrack = null;
    protected String _curTrackName = null;
    protected int _trackSplitSeconds = 300;
    protected Date _lastPointDate = null;
    protected File _file = null;
    

    public abstract boolean readTrack();
    public abstract boolean readTrack(File file);

    public static final int STARTED = 0, USINGLOOKUP = 1, COMPLETE = 2;

    public Track getTrack(int index) {
        if ((index < 0) || (index >= _tracks.size())) return null;
        Track track = (Track)_tracks.get(index);
        track.sort();
        return track;
    }

    public int getTrackCount() {
        return _tracks.size();
    }

    public ArrayList<Track> getTracks() {
        return _tracks;
    }

    public ArrayList<Track> getTracks(Extent boundingbox) {
        ArrayList<Track> tracks = new ArrayList<Track>();
        for (GpsDatatype obj : _tracks) {
        	Track track = (Track)obj;
            if (track.Extent().intersects(boundingbox)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    public ArrayList<GpsDatatype> getTrackGDT(Extent boundingbox, boolean limitToExtent) {
        ArrayList<GpsDatatype> tracks = new ArrayList<GpsDatatype>();
        for (GpsDatatype obj : _tracks) {
        	Track track = (Track)obj;
            if ((!limitToExtent) || track.Extent().intersects(boundingbox)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    public static TrackReader getTrackReader(File file) {
        return resolveReader(FileUtil.getExtention(file));
    }

    public static TrackReader resolveReader(String extention) {
        if (extention == null) return null;
        extention = extention.toUpperCase();
        TrackReader reader = null;

        try {
            String className = TRACK_READER_ROOT + ".TrackReader" + extention;
            @SuppressWarnings("unchecked")
			Class<Object> readerType = (Class<Object>)Class.forName(className);
            reader = (TrackReader)readerType.newInstance();

        } catch (Exception e) {
        	return null;
        }
        return reader;
    }

    protected boolean addTrackPoint(GpsLocation tp) {
        boolean newTrack = false;
        try {
            if (tp != null)  {
                if ((_curTrack == null) || (tp.startMillis() - _lastPointDate.getTime()) > (_trackSplitSeconds * 1000)) {
                    _curTrack = new Track();
                    newTrack = true;
                    String prefix = FileUtil.getPrefix(_file);
                    _curTrack.Name( prefix + "_" + _tracks.size());
                    _curTrack.Filename( _file.getName());
                    _tracks.add(_curTrack);
                }
                _lastPointDate = tp.Utc();
                _curTrack.add(tp);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return newTrack;
    }

    protected void resort() {
        for (GpsDatatype obj : _tracks) {
        	Track track = (Track)obj;
            track.sort();
        }
    }


//    public static Date DecimaDaysToDateTime(double value) {
//        Date startDate = new Date(1899, 12, 30, 0, 0, 0);
//
//        // Take the integer part of our given value and apply that
//        // number of days.
//        int days = (int)value;
//        double tod = value - days;
//
//        if (tod > 0.0) {
//            tod *= 24.0;
//            int hrs = (int)tod;
//            tod -= hrs;
//            tod *= 60.0;
//            int min = (int)tod;
//            tod -= min;
//            tod *= 60.0;
//            int secs = (int)tod;
//            tod -= secs;
//            int millis = (int)(tod * 1000);
//            startDate = startDate.AddDays(days);
//            startDate = startDate.AddHours(hrs);
//            startDate = startDate.AddMinutes(min);
//            startDate = startDate.AddSeconds(secs);
//            startDate = startDate.AddMilliseconds(millis);
//        }
//
//        return startDate;
//    }

//    public static Date getDateISO8601(String strDate) {
//        try {
//            Date dt = Date.ParseExact(strDate, "yyyy-MM-ddTHH:mm:ssZ", System.Globalization.CultureInfo.InvariantCulture);
//            return dt.ToUniversalTime();
//
//        } catch (Exception e) {
//            DebugView.WriteLine(e.Message);
//            return Date.MinValue;
//        }
//    }


}
