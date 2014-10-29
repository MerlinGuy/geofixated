package org.geof.gps.data;

import java.util.Date;

public class TimeFrame {

    private final static long mPerHour = 1000*60*60;
    private final static long mPerMinute = 1000*60;
    
    protected Date _starttime = new Date();
    protected Date _endtime = new Date();

    //-------------------------------------------------------
    public TimeFrame() { }

    public TimeFrame(Date starttime, Date endtime) {
        initialize(starttime, endtime);
    }
    
    public TimeFrame(long start, long end) {
        _starttime = new Date(start);
        _endtime = new Date(end);
    }

    //-------------------------------------------------------
    protected void initialize(Date starttime, Date endtime) {
        _starttime = starttime;
        _endtime = endtime;
    }

    public Date StartTime() {
    	return _starttime;
    }
    
    public Date EndTime() {
    	return _endtime;
    }
    
    public void StartTime(Date date) {
    	_starttime = date;
    }
    
    public void EndTime(Date date) {
    	_endtime = date;
    }
    
    public Long TimeSpan() {
    	if ((_starttime != null ) && (_endtime != null)) {
    		return _endtime.getTime() - _starttime.getTime();
    	} else {
    		return null;
    	}
    }
    
    public TimeFrame clone() {
        return new TimeFrame(_starttime, _endtime);
    }

    public void adjust(Date start, Date end) {
        _starttime = start;
        _endtime = end;
    }

    public void adjust(Date start, long timespan) {
        _starttime = start;
        _endtime = new Date(_starttime.getTime() + timespan);
    }

    public void adjust(Date datetime) {
    	adjust(datetime, datetime);
    }

    public void adjust(TimeFrame timeframe) {
    	adjust(timeframe.StartTime(), timeframe.EndTime());
    }

    public void adjust(TimeFrame timeframe, long timespan) {
    	adjust(timeframe.StartTime(), timespan);
    }

    public boolean overLaps(Date start, Date end) {
        if ((start.getTime() >= _starttime.getTime()) && (start.getTime() <= _endtime.getTime())) return true;
        if ((end.getTime() >=  _starttime.getTime()) && (end.getTime() <= _endtime.getTime())) return true;
        if (( _starttime.getTime() >= start.getTime()) && ( _starttime.getTime() <= end.getTime())) return true;
        if ((_endtime.getTime() >= start.getTime()) && (_endtime.getTime() <= end.getTime())) return true;
        return false;
    }

    public boolean overLaps(TimeFrame tf) {
    	return overLaps(tf.StartTime(), tf.EndTime());
    }

    public boolean Overlaps(Date dt) {
        return ((dt.getTime() >= _starttime.getTime()) && (dt.getTime() <= _endtime.getTime()));
    }

    public Date[] calcPointsInTime(int pointCount, boolean firstAtZero) {
    	long incMillis = this.TimeSpan() / (firstAtZero ? pointCount -1 : pointCount);
    	long startMillis = _starttime.getTime() + (firstAtZero ? 0 : incMillis);
    	Date[] dates = new Date[pointCount];
    	dates[0] = new Date(startMillis);
    	for (int indx = 1; indx < pointCount; indx++) {
    		startMillis += incMillis;
    		dates[indx] = new Date(startMillis);
    	}
    	return dates;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	Long millis = TimeSpan();
    	if (millis == null) {
    		return "--:--";
    	}
    	long Hours = millis / mPerHour;
    	long Minutes = (millis % mPerHour) / mPerMinute;
		long Seconds = ((millis % mPerHour) % (mPerMinute)) / 1000;
		sb.append(Hours).append("h ");
		sb.append(Minutes).append("m ");
		sb.append(Seconds).append("s ");
		return sb.toString();
    }

	public static Date add(Date date, TimeFrame timeframe) {
		return new Date(date.getTime() + timeframe.TimeSpan());
	}

	public static Date subtract(Date date, TimeFrame timeframe) {
		return new Date(date.getTime() - timeframe.TimeSpan());
	}

}
