package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.geof.db.ColumnTrio;
import org.geof.db.QueryBuilder;
import org.geof.job.LinepointTask;
import org.geof.job.TaskExecutor;
import org.geof.util.DateUtil;
import org.json.JSONArray;

/**
 * Derived FileInfo class to handle database interactions for the Linepoint table
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class LinepointRequest extends DBRequest {

	private final static String ENTITYNAME = "linepoint";
	
	public final static String LINEID = "lineid";
	public final static String FILEID = "fileid";
	public final static String CNT = "cnt";
	public final static String LINEPOINTS = "linepoints";
	
	private final static String _sqlClean = "DELETE FROM linepoint WHERE lineid NOT IN (SELECT id FROM line)";
//	private final static String _sqlDelete = "DELETE FROM linepoint WHERE lineid = ?";
//	private final static String _tpInsert = "INSERT INTO LinePoint (lineid,ordernum,longitude,latitude,utcdate,timeoffset,altitude,distance,azimuth) VALUES (?,?,?,?,?,?,?,?,?)";
	private final static String _sqlCount = "SELECT count(lineid) as cnt FROM linepoint WHERE lineid = ?";
	private final static String _sqlCountFile = "SELECT count(linepoint.lineid) as cnt FROM linepoint, file_line WHERE file_line.fileid = ? AND file_line.lineid = linepoint.lineid";
	private final static String _sqlFromFile = " FROM linepoint, file_line WHERE file_line.fileid = ? AND file_line.lineid = linepoint.lineid ORDER BY ordernum";

	private final static String DISTANCE = "distance";
	private final static String TIMESPAN = "timespan";

	/**
	 * Class constructor
	 */
	public LinepointRequest() {
		_entityname = ENTITYNAME;
	}

	/**
	 * Overrides the base class process() to provide Linepoint specific action handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(Request.CREATE)) {
			return super.create();

		} else if (_actionKey.equalsIgnoreCase("create.compressed")) {
			return createCompressed();			
		}  else if (_actionKey.equalsIgnoreCase("read.compressed")) {
			return readCompressed();
		} else if (_actionKey.equalsIgnoreCase(Request.READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase("read.count")) {
			return readCount();
		} else {
			return super.process();
		}
	}

	/**
	 * Removes orphaned linepoints from the Linepoint.
	 * 
	 * @return Always returns true.
	 */
	public boolean clean() {
		try {
			_dbi.executePL(_sqlClean, null);
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}
	
	public boolean createCompressed() {
		try {			
			JSONArray linepoints = _fields.getJSONArray(LINEPOINTS);
			Long fileid = _fields.getLong(FILEID);
			LinepointTask lt = new LinepointTask(fileid, linepoints, true);
			TaskExecutor.execute(lt);
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}

	}

	/**
	 * Queries the Linepoint table and returns all the points that conform to the search
	 * criteria specified in the _data object.
	 * 
	 * @return Returns true if no error occurs, otherwise false.
	 */
	protected boolean read() {
		boolean rtn = false;
		ResultSet rs = null;
		try {
			double distance = _data.optDouble( DISTANCE, -1.0);
			long timespan = _data.optLong( TIMESPAN, -1);

			if (_where.has(FILEID)) {
				String sql = "SELECT " + QueryBuilder.buildColumn(_entitymap, _data, true) + _sqlFromFile;
				PreparedStatement ps = null;
				ps = _dbi.getPreparedStatement(sql);
				ps.setLong(1, _where.getLong(FILEID));
				rs = ps.executeQuery();
				_jwriter.writePair(FILEID, _where.getInt(FILEID));
			} else {
				rs = _dbi.readRecords(_entitymap, _data);
			}
			
			_jwriter.openArray(DATA);
			String[] fields = _entitymap.getDefaultFields();

			if ((distance < 0) && (timespan < 0)) {
				_jwriter.writeResultset(rs, fields);
			} else {
				ColumnTrio[] cps = ColumnTrio.getList(rs.getMetaData(), fields);
				
				double curDist = distance + 1;
				long curSpan = timespan + 1;
				long nextSpan = timespan;

				double pointcount = _data.optInt( "pointcount", -1);
				int curpoint = 0;
				boolean isLast = false;
				while (rs.next()) {
					isLast = (curpoint == pointcount - 1);
					if (timespan > -1) {
						if ((curSpan > nextSpan) || (isLast)) {
							_jwriter.writeRecordset(rs, cps);
							nextSpan = rs.getTimestamp("utcdate").getTime() + timespan;
						} else {
							curSpan = rs.getTimestamp("utcdate").getTime();
						}
					} else {
						if ((curDist > distance) || (isLast)) {
							_jwriter.writeRecord(rs, cps);
							curDist = 0;
						} else {
							curDist += rs.getDouble("distance");
						}
					}
					curpoint++;
				}
			}
			_jwriter.closeArray();
		} catch (Exception e) {
			setError(e.getMessage());

		} finally {
			if (rs != null) {
				try { rs.close();} catch (Exception dbe){}
			}
		}
		return rtn;
	}

	protected boolean readCount() {
		boolean rtn = false;
		try {
			String field;
			String sql;
			if ( _where.has(LINEID)) {
				field = LINEID;
				sql = _sqlCount;
				
			} else if (_where.has(FILEID)) {
				field = FILEID;
				sql = _sqlCountFile;				
				
			} else {
				setError("Linepoint.read.count missing both lineid or fileid");
				return false;
			}
			int id = _where.getInt(field);
			PreparedStatement ps = null;
			ps = _dbi.getPreparedStatement(sql);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			_jwriter.openObject(DATA);
			if (rs.next()) {
				_jwriter.writePair(field, id);
				_jwriter.writePair(CNT, rs.getInt(CNT));
			}
			rs.close();
			_jwriter.closeObject();

		} catch (Exception e) {
			setError(e);
		}
		return rtn;
	}
	
	/**
	 * Returns all the linepoints in sorted order which are linked to the parent line in
	 * listed in the _data object's Where section. The information returned are in CSV instead
	 * of JSONObject format to speed up the query and use less bytes during transmission.
	 * 
	 * @return Returns true if no error occurs, otherwise false.
	 */
	protected boolean readCompressed() {
		boolean rtn = false;
		ResultSet rs = null;
		try {
			rs = _dbi.readRecords(_entitymap, _data);
			StringBuilder sb = new StringBuilder();
			String comma = ",";
			String space = " ";
			int count = 0;
			String date ;
			while (rs.next()) {
				date = DateUtil.DFISO8601.format(rs.getTimestamp("utcdate"));
				if (count > 0) {
					sb.append(comma);
				}
				sb.append(rs.getDouble("latitude"))
				.append(space).append(rs.getDouble("longitude"))
				.append(space).append(date);
				count++;
			}
			rs.close();

			_jwriter.openObject(DATA);
			_jwriter.writePair("compressed", "true");
			_jwriter.writePair("linepoints", sb.toString());
			_jwriter.closeObject();
			rtn = true;

		} catch (Exception e) {
			setError(e);
		} 
		return rtn;
	}
	
//	public static void deleteLinePoints(long lineid, DBConnection conn){
//		PreparedStatement ps = null;
//		try {
//			ps = conn.getPreparedStatement(_sqlDelete);
//			ps.setLong(1, lineid);
//			ps.execute();
//		} catch (Exception e) {
//			GLogger.error(e);
//		} finally {
//			conn.closeStatement(ps);
//		}
//	}
//
//	public static void addLinePoints(long lineid, JSONArray tpoints, DBConnection conn){
//		PreparedStatement ps = null;
//		try {
//			ps = conn.getPreparedStatement(_tpInsert);
//			long pointcount = tpoints.length();
//			JSONArray columns;
//			for (int ordernum = 0; ordernum < pointcount; ordernum++) {
//				columns = tpoints.getJSONArray(ordernum);
//				ps.clearParameters();
//				//lineid, ordernum, longitude,latitude,utcdate,altitude
//				ps.setLong(1, lineid);
//				ps.setLong(2, ordernum);
//				ps.setDouble(3, columns.getDouble(0));
//				ps.setDouble(4, columns.getDouble(1));
//				ps.setTimestamp(5, Timestamp.valueOf(columns.getString(2)));
//				ps.setDouble(6, columns.getDouble(3));
//				ps.execute();
//			}
//		} catch (Exception e) {
//			GLogger.error(e);
//		} finally {
//			conn.closeStatement(ps);
//		}
//	}
//	
//	public static boolean addCompressed(JSONArray linepoints, DBConnection conn){
//		//format: lineid,ordernum,longitude,latitude,utcdate,timeoffset,altitude,distance,azimuth
//		// ['123,0,-50.123,44.123,2013-02-02 17:34:52,2.1,0.0,1.23,0.0'},
//		//	'123,0,-50.123,44.123,2013-02-02 17:34:52,2,8,0.0,1.23,0.0']
//		PreparedStatement ps = null;
//		try {
//			ps = conn.getPreparedStatement(_tpInsert);
//			long pointcount = linepoints.length();
//			String[] vals;
//			for (int indx = 0; indx < pointcount; indx++) {
//				vals = linepoints.getString(indx).split(",");				
//				ps.clearParameters();
//				ps.setLong(1, Long.parseLong(vals[0]));
//				ps.setInt(2, Integer.parseInt(vals[1]));
//				ps.setDouble(3, Double.parseDouble(vals[2]));
//				ps.setDouble(4,  Double.parseDouble(vals[3]));
//				ps.setTimestamp(5, Timestamp.valueOf(vals[4]));
//				ps.setDouble(6,  Double.parseDouble(vals[5]));
//				ps.setDouble(7,  Double.parseDouble(vals[6]));
//				ps.setDouble(8,  Double.parseDouble(vals[7]));
//				ps.setDouble(9,  Double.parseDouble(vals[8]));
//				ps.execute();
//			}
//			return true;
//		} catch (Exception e) {
//			GLogger.error(e);
//			return false;
//		} finally {
//			conn.closeStatement(ps);
//		}
//	}
}
