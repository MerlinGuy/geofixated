package org.geof.gps.data;

import java.awt.geom.Line2D;

public class Extent {

	private double _minLat = 0.0;
	private double _minLon = 0.0;
	private double _maxLat = 0.0;
	private double _maxLon = 0.0;
	private boolean _hasMinLat = false;
	private boolean _hasMinLon = false;
	private boolean _hasMaxLat = false;
	private boolean _hasMaxLon = false;
	
	public Extent() {}
	
	public Extent(double minLat, double minLon, double maxLat, double maxLon) {
		this.MinLat( minLat);
		this.MinLon(minLon);
		this.MaxLat(maxLat);
		this.MaxLon(maxLon);
	}
	
	public double MinLat() {
		return _minLat;
	}
	
	public void MinLat(double value) {
		if ((!_hasMinLat) || (value < _minLat)) { 
			_minLat = value;
			_hasMinLat = true;
		}
	}
	
	public double MinLon() {
		return _minLon;
	}
	
	public void MinLon(double value) {
		if ((!_hasMinLon) || (value < _minLon)) { 
			_minLon = value;
			_hasMinLon = true;
		}
	}
	
	public double MaxLat() {
		return _maxLat;
	}
	
	public void MaxLat(double value) {
		if ((!_hasMaxLat) || (value > _maxLat)) { 
			_maxLat = value;
			_hasMaxLat = true;
		}
	}
	
	public double MaxLon() {
		return _maxLon;
	}
	
	public void MaxLon(double value) {
		if ((!_hasMaxLon) || (value > _maxLon)) { 
			_maxLon = value;
			_hasMaxLon = true;
		}
	}
	
	public boolean validExtent() {
		return _hasMinLat &&_hasMinLon && _hasMaxLat && _hasMaxLon;
	}
	
	public Location getCenterPoint() {
		double latitude = (_maxLat + _minLat) / 2.0;
		double longitude = (_maxLon + _minLon) / 2.0;
		return new Location (latitude, longitude);
	}
	
	public Location[] getCorners() {
		Location[] corners = new Location[4];
		corners[0] = new Location(_minLat, _minLon);
		corners[1] = new Location(_minLat, _maxLon);
		corners[2] = new Location(_maxLat, _minLon);
		corners[3] = new Location(_maxLat, _maxLon);
		return corners;
	}
	
	public Line2D[] getLines() {
		Line2D[] lines = new Line2D[4];
		lines[0] = new Line2D.Double(_minLat, _minLon,_minLat, _maxLon);
		lines[1] = new Line2D.Double(_minLat, _maxLon,_maxLat, _minLon);
		lines[2] = new Line2D.Double(_maxLat, _minLon,_maxLat, _maxLon);
		lines[3] = new Line2D.Double(_maxLat, _maxLon,_minLat, _minLon);
		return lines;
	}
	
	public boolean intersects(Location location) {
		return ((location.Latitude() >= _minLat)
				&& (location.Latitude() <= _maxLat)
				&& (location.Longitude() >= _minLon)
				&& (location.Longitude() <= _maxLon));
	}
	
	public boolean intersects(GpsLocation tp) {
		return intersects( tp );
	}
	
	public boolean intersects(Track track) {
		for (GpsLocation tp : track.TrackPoints()) {
			if (intersects(tp)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean intersects(Extent extent) {
		for (Location corner : extent.getCorners()) {
			if (intersects(corner)) {
				return true;
			}
		}
		for (Location corner : getCorners()) {
			if (extent.intersects(corner)) {
				return true;
			}
		}
		for ( Line2D myline : getLines()) {
			for ( Line2D theirline :  extent.getLines()) {
				if (myline.intersectsLine(theirline)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
}
