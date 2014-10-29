package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

import org.geof.db.ColumnTrio;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.util.GeometryUtil;
import org.json.JSONObject;

/**
 * Derived FileInfo class to handle database interactions for the Track table
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class LineRequest extends DBRequest {

	public final static String ENTITYNAME = "line";
	
	public final static String DISTANCE = "distance";
	public final static String RESEQUENCE = "resequence";
	public final static int MAXPOINTCOUNT = 1000;
	public final static String LINEID = "lineid";
	public final static String LINEPOINT = "linepoint";
	
	protected String PKEYFIELD = "id";

//	private final static String _sqlCreateDate = "select min(utcdate) as firstdate from linepoint where lineid = ?";
	
	private final static String _sqlSelectLinePoint = "SELECT id FROM linepoint WHERE lineid =? ORDER BY utcdate";
	private final static String _sqlUpdateLinePoint = "UPDATE linepoint SET ordernum = ? WHERE id = ?";
	private final static String _sqlSetDistance = "UPDATE linepoint lp SET distance = ST_Distance_Sphere(geom,"
													+ " (SELECT geom FROM linepoint WHERE lineid = lp.lineid "
													+ "	AND ordernum = lp.ordernum + 1)) WHERE lp.lineid = ?";

	/**
	 * Class constructor.
	 */
	public LineRequest() {
		_entityname = ENTITYNAME;
//		initialize();
//		LISTFIELDS = new String[] { "id", "filename", "status" };
	}

	/**
	 * Overrides the base class process() to provide Track specific action handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		try {
			 if (_actionKey.equalsIgnoreCase(CREATE)) {

				return create();

			} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
				boolean rtn = false;

				if (_data.has(RESEQUENCE)) {
					rtn = resequence();
				} else {
					rtn = super.update();
				}
				return rtn;

			} else if (_actionKey.equalsIgnoreCase(DELETE)) {
				return super.delete();

			} else {
				return super.process();
			}
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	/**
	 * Creates a new Track record
	 * 
	 * @return Returns true if no error occurs, otherwise false.
	 */
	public boolean create() {
		try {
			super.create();
//			long lineid = _dbi.getReturnedKeys().getLong(LINEID);
			// if (_data.has("compressed") && _data.getBoolean("compressed") &&
			// _data.has("linepoints")) {
			// TrackpointRequest.deleteTrackPoints(lineid, _conn);
			// JSONArray points = _data.getJSONObject("linepoints").getJSONArray("points");
			// TrackpointRequest.addTrackPoints(lineid, points, _conn);
			// }
//			_jwriter.openObject(DATA);
//			_jwriter.writePair(LINEID, lineid);
//			_jwriter.closeObject();
			
//			if (rtn) {
//				updateGeometries(lineid, false);
//			}
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	/**
	 * Creates a new Track table record using information in passed in linedata object
	 * instead of the default _data object
	 * 
	 * @param linedata JSONObject to use for Track table values
	 * @return Returns the Track id / Primary Key for the new line record
	 */
	public long create(JSONObject linedata) {
		boolean rtn = false;
		try {
			rtn = _dbi.create(_entitymap, linedata);
			if (rtn) {
				long lineid = _dbi.getLastReturning().getLong(ID);
				updateGeometries(lineid, false);
				return lineid;
			}
		} catch (Exception e) {
			setError(e);
		}
		return -1;
	}

	/**
	 * Queries the TrackPoint table for all the linepoints for the line referenced in the
	 * _data JSONObject and then writes them to the response stream.
	 * 
	 * @return Returns true if no error occurs, otherwise false.
	 */
	protected boolean linepoint() {
		boolean rtn = false;
		try {
			double distance = _data.optDouble( DISTANCE, -1.0);
			_data.put("orderby", "ordernum");
			JSONObject where = _data.getJSONObject("where");
			long id = where.getLong("id");
			where.remove("id");
			where.put(PKEYFIELD, id);
			_jwriter.writePair(PKEYFIELD, id);
			if (_data.has("searchid")) {
				_jwriter.writePair("searchid", _data.getLong("searchid"));
			}

			EntityMap emap = EntityMapMgr.getEMap(LINEPOINT);
			ResultSet rs = _dbi.readRecords(emap, _data);
			_jwriter.openArray(LINEPOINT);

			String[] fields = emap.getDefaultFields();

			if (distance < 0) {
				_jwriter.writeResultset(rs, fields);
			} else {
				ResultSetMetaData rsmd = rs.getMetaData();
				ColumnTrio[] cps = ColumnTrio.getList(rsmd, fields);

				double curDist = distance + 1;
				double pointcount = _data.optInt( "pointcount", -1);
				int curpoint = 0;
				boolean isLast = false;
				while (rs.next()) {
					isLast = (curpoint == pointcount - 1);
					if ((curDist > distance) || (isLast)) {
						_jwriter.writeRecord(rs, cps);
						curDist = 0;
					} else {
						curDist += rs.getDouble("distance");
					}
					curpoint++;
				}
			}
			_jwriter.closeArray();
		} catch (Exception e) {
			setError(e);
		} finally {
			_dbi.closeAll();
		}
		return rtn;
	}

	/**
	 * This method queries for all the linepoints associated with a line referenced by the
	 * _data JSONObject and resequences them according to their utcdate field.
	 * 
	 * NOTE: This method can take quite a while and consume alot of CPU if the line has a
	 * substantial number of linepoints.
	 * 
	 * @return Returns true if no error occurs, otherwise false.
	 */
	protected boolean resequence() {
		try {
			long lineid = _where.getLong("id");
			boolean geometryonly = false;
			if (_data.has("geometryonly")) {
				geometryonly = _data.getBoolean("geometryonly");
			}

			if (! geometryonly) {
				ArrayList<Long> ids = new ArrayList<Long>();
				PreparedStatement ps = _dbi.getPreparedStatement(_sqlSelectLinePoint);
				ps.setLong(1, lineid);
				ResultSet rs = ps.executeQuery();
	
				while (rs.next()) {
					ids.add(rs.getLong(1));
				}
				rs.close();
				ps.close();
	
				ps = _dbi.getPreparedStatement(_sqlUpdateLinePoint);
	
				int count = ids.size();
				for (int indx = 0; indx < count; indx++) {
					ps.setInt(1, indx);
					ps.setLong(2, ids.get(indx));
					ps.execute();
				}
				ps.close();
				
				ps = _dbi.getPreparedStatement(_sqlSetDistance);
				ps.setLong(1, lineid);
				ps.execute();
				ps.close();
				
			}
			updateGeometries(lineid, geometryonly);
			return true;

		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}

	/**
	 * Queries the Trackpoints for a Track Object and then rebuilds the spatial "geom"
	 * linestring field of the Track object.
	 * 
	 * @param lineid Primary key of the line whose geom field is to be updated.
	 * 
	 * @return Returns true if no error occurs, otherwise false.
	 */
	protected void updateGeometries(long lineid, boolean geometryonly) {
		GeometryUtil.RebuildLineGeometry(lineid, geometryonly);
	}
	
//	public static boolean deploy(DBInteract dbi, FileUpload fu) {
//		try {
//			long fileid = fu.PKey;
//			String ext = FileUtil.getExtention(fu.OriginalName);
//			File file = fu.getFile(fu.NewName);
//			JSONObject jGeo = GeoUtil.saveGeometryToDb(fu.FileType, file, ext, dbi);
//			
//			// 1) Set create date from first point.
//			long lineid = jGeo.getLong("geomid");
//			String utcdate = jGeo.getString("utcdate");
////			String geotype = jGeo.getString("geomtype");
//			JSONObject data = JsonUtil.getDataFields(fileid);
//			data.getJSONObject(FIELDS).put(CREATEDATE, utcdate);
//			dbi.update(EntityMapMgr.getEMap(FileRequest.ENTITYNAME), data, false);
//
//			// 2) create link in the file_line table to the line
//			DBLink.AddLink(FileRequest.ENTITYNAME, fileid, ENTITYNAME, lineid, dbi.getConnection());
//
////			// 3) set file status to ONLINE
////			return setMediaStatus(EntityMapMgr.getEMap(FileRequest.ENTITYNAME), fileid, ONLINE);
//		} catch (Exception e) {
//			GLogger.error(e);
//			GLogger.error(e.getMessage());
//		}
//		return false;
//	}
	
//		try {
//			// GLogger.error("updateGeometries - lineid: " + lineid);
//			if (lineid == -1) {
//				if (_data.has(FIELDS)) {
//					lineid = _data.getJSONObject(FIELDS).getLong(ID);
//				} else if (_data.has(WHERE)) {
//					lineid = _data.getJSONObject(WHERE).getLong(ID);
//				}
//			}
//
//			/**
//			 * Get the count of the TrackPoints.
//			 */
//			int pointcount = -1;
//			String sql = "SELECT count(lineid) FROM TrackPoint WHERE lineid = ?";
//			PreparedStatement ps = _conn.getPreparedStatement(sql);
//			ps.setLong(1, lineid);
//			ResultSet rs = ps.executeQuery();
//			if ((rs != null) && rs.next()) {
//				pointcount = rs.getInt(1);
//			}
//			rs.close();
//			ps.close();
//
//			if (pointcount < 1) {
//				return false;
//			}
//			int usePoint = (int) (pointcount / MAXPOINTCOUNT);
//			if ((pointcount % MAXPOINTCOUNT) > (((double)MAXPOINTCOUNT) * .49)) {
//				usePoint++;
//			}
//			boolean useAll = pointcount < MAXPOINTCOUNT;
//
//			/**
//			 * Update the geom field with a linestring.
//			 */
//			sql = "SELECT longitude,latitude FROM TrackPoint WHERE lineid = ? ORDER BY ordernum";
//			ps = _conn.getPreparedStatement(sql);
//			ps.setLong(1, lineid);
//			rs = ps.executeQuery();
//			ArrayList<Point> points = new ArrayList<Point>();
//			Number lat, lon;
//			int curpoint = 0;
//			pointcount--;
//			int pointsAdded = 0;
//
//			// GLogger.verbose("useAll: " + useAll + ", usePoint: " + usePoint + ", pointcount: " + pointcount);
//			while (rs.next()) {
//				if (useAll || (curpoint == pointcount) || (curpoint % usePoint == 0)) {
//					lon = dfLatLon.parse(dfLatLon.format(rs.getDouble(1)));
//					lat = dfLatLon.parse(dfLatLon.format(rs.getDouble(2)));
//					points.add(new Point(lon.doubleValue(), lat.doubleValue()));
//					pointsAdded++;
//				}
//				curpoint++;
//			}
//			rs.close();
//			ps.close();
//			Point[] pointArray = new Point[points.size()];
//			points.toArray(pointArray);
//
//			// GLogger.verbose("Updating the Track.geom field for id = " + lineid +
//			// ", linestring size = " + pointsAdded);
//			LineString ls = new LineString(pointArray);
//			ls.srid = GlobalProperties.getInt(POINTSRID, DEFAULTSRID);
//			((ConnectionPG) _conn).addPGeometry();
//			sql = "UPDATE Track SET geom = ? WHERE id = ?";
//			ps = _conn.getPreparedStatement(sql);
//			ps.setObject(1, ls, GEOMETRYDATATYPE);
//			ps.setLong(2, lineid);
//			ps.execute();
//			ps.close();
//			// GLogger.verbose("Track.geom updated");
//
//			if (!geometryonly) {
//				/**
//				 * Update the Geometry of TrackPoint
//				 */
//				sql = "UPDATE TrackPoint SET geom = geomfromtext('POINT('||longitude||' '||latitude||')', ?) WHERE lineid = ?";
//				ps = _conn.getPreparedStatement(sql);
//				ps.setInt(1, GlobalProperties.getInt(POINTSRID, DEFAULTSRID));
//				ps.setLong(2, lineid);
//				ps.execute();
//				ps.close();
//
//				/**
//				 * Update the Distances
//				 */
//				sql = "UPDATE linepoint t1 SET distance = ST_distance_sphere((SELECT t2.geom FROM linepoint t2 WHERE t2.lineid = t1.lineid" + " AND t2.ordernum = t1.ordernum -1), geom) WHERE t1.lineid = ?";
//				ps = _conn.getPreparedStatement(sql);
//				ps.setLong(1, lineid);
//				ps.execute();
//				ps.close();
//
//				/**
//				 * Update Start and End date of Track with first and last Trackpoint dates
//				 */
//				sql = "UPDATE Track t SET startdate = (SELECT Min(tp1.utcdate) FROM TrackPoint tp1 WHERE tp1.lineid = t.id),"
//						+ " enddate = (SELECT Max(tp2.utcdate) FROM TrackPoint tp2 WHERE tp2.lineid = t.id) WHERE t.id = ?";
//				ps = _conn.getPreparedStatement(sql);
//				ps.setLong(1, lineid);
//				ps.execute();
//				ps.close();
//			}
//			return true;
//		} catch (Exception e) {
//			GLogger.error(e);
//			return false;
//		}
//	}

}
