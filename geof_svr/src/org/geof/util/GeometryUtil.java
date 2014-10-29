package org.geof.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.geof.db.ConnectionPG;
import org.geof.db.DBConnMgr;
import org.geof.db.DBConnection;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.data.FileInfo;
import org.geof.request.LineRequest;
import org.postgis.LineString;
import org.postgis.Point;

public class GeometryUtil implements Runnable {
	
	public static final DecimalFormat DFLatLon = new DecimalFormat("0.000000");
	private long _lineid = -1;
	private boolean _geometryonly = true;
	private DBConnection _conn = null;
	
	//------------------------------------------
	private GeometryUtil() {	}
	//------------------------------------------
	
	public static void RebuildLineGeometry(long lineid, boolean geometryonly) {
		GeometryUtil gs = new GeometryUtil();
		gs.rebuild(lineid,geometryonly);
	}
	
	private void rebuild(long lineid, boolean geometryonly) {
		if (lineid > -1) {
			_lineid = lineid;
			_geometryonly = geometryonly;
			Thread t = new Thread (this);
	        t.start();
		}
	}
	
	@Override
	public void run() {
		try {
			_conn = DBConnMgr.getConnection();
			//GLogger.verbose("GeometryService.run - lineid: " + _lineid);
			// Take Line off line.
			setLineStatus( 0 );
			/**
			 * Get the count of the LinePoints.
			 */
			int pointcount = -1;
			String sql = "SELECT count(lineid) FROM LinePoint WHERE lineid = ?";
			PreparedStatement ps = _conn.getPreparedStatement(sql);
			ps.setLong(1, _lineid);
			ResultSet rs = ps.executeQuery();
			if ((rs != null) && rs.next()) {
				pointcount = rs.getInt(1);
			}
			rs.close();
			ps.close();

			if (pointcount < 1) {				
				return;
			}
			
			int maxpointcount = LineRequest.MAXPOINTCOUNT;
			int usePoint = (int) (pointcount / maxpointcount);
			if ((pointcount % maxpointcount) > (((double)maxpointcount) * .49)) {
				usePoint++;
			}
			boolean useAll = pointcount < maxpointcount;

			/**
			 * Update the geom field with a linestring.
			 */
			sql = "SELECT longitude,latitude FROM LinePoint WHERE lineid = ? ORDER BY ordernum";
			ps = _conn.getPreparedStatement(sql);
			ps.setLong(1, _lineid);
			rs = ps.executeQuery();
			ArrayList<Point> points = new ArrayList<Point>();
			Number lat, lon;
			int curpoint = 0;
			pointcount--;
//			int pointsAdded = 0;

			//GLogger.verbose("useAll: " + useAll + ", usePoint: " + usePoint + ", pointcount: " + pointcount);
			while (rs.next()) {
				if (useAll || (curpoint == pointcount) || (curpoint % usePoint == 0)) {
					lon = DFLatLon.parse(DFLatLon.format(rs.getDouble(1)));
					lat = DFLatLon.parse(DFLatLon.format(rs.getDouble(2)));
					points.add(new Point(lon.doubleValue(), lat.doubleValue()));
//					pointsAdded++;
				}
				curpoint++;
			}
			rs.close();
			ps.close();
			Point[] pointArray = new Point[points.size()];
			points.toArray(pointArray);

			//GLogger.verbose("Updating Line.geom field for: " + _lineid + ", linestring size: " + pointsAdded);
			LineString ls = new LineString(pointArray);
			ls.srid = GlobalProp.instance().getInt(FileInfo.POINTSRID, FileInfo.DEFAULTSRID);
			((ConnectionPG) _conn).addPGeometry();
			sql = "UPDATE Line SET geom = ? WHERE id = ?";
			ps = _conn.getPreparedStatement(sql);
			ps.setObject(1, ls, FileInfo.GEOMETRYDATATYPE);
			ps.setLong(2, _lineid);
			ps.execute();
			ps.close();
			//GLogger.verbose("Line.geom updated");

			if (!_geometryonly) {
				/**
				 * Update the Geometry of LinePoint
				 */
				sql = "UPDATE LinePoint SET geom = geomfromtext('POINT('||longitude||' '||latitude||')', ?) WHERE lineid = ?";
				ps = _conn.getPreparedStatement(sql);
				ps.setInt(1, GlobalProp.instance().getInt(FileInfo.POINTSRID, FileInfo.DEFAULTSRID));
				ps.setLong(2, _lineid);
				ps.execute();
				ps.close();

				/**
				 * Update the Distances
				 */
				sql = "UPDATE linepoint t1 SET distance = ST_distance_sphere((SELECT t2.geom FROM linepoint t2 WHERE t2.lineid = t1.lineid" + " AND t2.ordernum = t1.ordernum -1), geom) WHERE t1.lineid = ?";
				ps = _conn.getPreparedStatement(sql);
				ps.setLong(1, _lineid);
				ps.execute();
				ps.close();

				/**
				 * Update Start and End date of Line with first and last Linepoint dates
				 */
				sql = "UPDATE Line t SET startdate = (SELECT Min(tp1.utcdate) FROM LinePoint tp1 WHERE tp1.lineid = t.id),"
						+ " enddate = (SELECT Max(tp2.utcdate) FROM LinePoint tp2 WHERE tp2.lineid = t.id) WHERE t.id = ?";
				ps = _conn.getPreparedStatement(sql);
				ps.setLong(1, _lineid);
				ps.execute();
				ps.close();
			}
			
			// Bring Line back on line.
			setLineStatus( 1 );
		} catch (Exception e) {
			GLogger.error(e);
		} finally {
			DBConnMgr.release(_conn);
			//GLogger.verbose("GeometryService.run - completed");
		}
	}
	
	private boolean setLineStatus( int status) {
		try {
			String sql = "UPDATE Line SET status = ? WHERE id = ?";
			PreparedStatement ps = _conn.getPreparedStatement(sql);
			ps.setInt(1, status);
			ps.setLong(2, _lineid);
			ps.execute();
			return true;

		} catch (Exception e) {
			GLogger.error(e);
			return false;
		} finally {
			_conn.closeAll();
		}
	}


	public void dispose() {
	}
	
	/**
	 * Finalize method for cleaning up the GeometryService after shutdown
	 */
	public void finalize() {
		try {
			dispose();
		} catch (Exception e) {
		}
	}
		
}
