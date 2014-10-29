package org.geof.job;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.geof.data.FileInfo;
import org.geof.db.DBInteract;
import org.geof.log.GLogger;
import org.geof.store.StorageMgr;
import org.json.JSONArray;

public class LinepointTask extends GeofTask {

	private Long _fileid = null;
	private JSONArray _linepoints = null;
	private boolean _compressed = false;
	
	private final static String _tpInsert = "INSERT INTO LinePoint" 
			+ " (lineid,ordernum,longitude,latitude,utcdate,altitude,distance,azimuth)"
			+ " VALUES (?,?,?,?,?,?,?,?)";
	
	private final static String _updOffset = "UPDATE Linepoint SET timeoffset ="  
	     + " extract(epoch from (utcdate - "
			+ "(SELECT utcdate FROM linepoint lp2 WHERE lp2.lineid = linepoint.lineid and lp2.ordernum = 0)"
			+ "))" 
			+ " WHERE lineid = ?";

	private final static String _sqlGetLineId = "SELECT lineid FROM file_line where fileid = ?";
	
	
	public LinepointTask(Long fileid, JSONArray linepoints, boolean compressed) {
		_fileid = fileid;
		_linepoints = linepoints;
		_compressed = compressed;
	}
	
	@Override
	public void doTask() {
		DBInteract dbi = new DBInteract();
		long lineid = getLineId(dbi);
		if (lineid > -1) {
			boolean rtn = false;
			if (_compressed) {
				rtn = addCompressed(dbi,lineid);
			} else {
				rtn = addLinePoints(dbi,lineid);
			}
			int status = rtn ? FileInfo.ONLINE : FileInfo.ERROR;
			StorageMgr.instance().setUploadStatus(_fileid, dbi, status, _error, false);
		}
		dbi.dispose();
	}

	public long getLineId(DBInteract dbi){
		PreparedStatement ps = null;
		long lineid = -1;
		try {
			ps = dbi.getPreparedStatement(_sqlGetLineId);
			ps.setLong(1, _fileid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				lineid = rs.getLong(1);
			}
			rs.close();
		} catch (Exception e) {
			GLogger.error(e);
			_error = e.getMessage();
		}
		return lineid;
	}

	public boolean addCompressed(DBInteract dbi, long lineid){
		PreparedStatement ps = null;
		String[] vals = null;
		int indx = 0;
		try {			
			ps = dbi.getPreparedStatement(_tpInsert);
			long pointcount = _linepoints.length();
			for (indx = 0; indx < pointcount; indx++) {
				vals = _linepoints.getString(indx).split(",");				
				ps.clearParameters();
				ps.setLong(1, lineid);
				ps.setInt(2, indx);
				ps.setDouble(3, Double.parseDouble(vals[0]));
				ps.setDouble(4,  Double.parseDouble(vals[1]));
				ps.setTimestamp(5, Timestamp.valueOf(vals[2]));
				ps.setDouble(6,  Double.parseDouble(vals[3]));
				ps.setDouble(7,  Double.parseDouble(vals[4]));
				ps.setDouble(8,  Double.parseDouble(vals[5]));
				ps.execute();
			}
			
			ps = dbi.getPreparedStatement(_updOffset);
			ps.setLong(1, lineid);
			ps.execute();
			
			return true;
		} catch (Exception e) {
			GLogger.error("LinepointTask addCompressed - " + String.valueOf(indx));
			_error = e.getMessage();
			return false;
		}
	}

	public boolean addLinePoints(DBInteract dbi, long lineid){
		PreparedStatement ps = null;
		try {
			ps = dbi.getPreparedStatement(_tpInsert);
			long pointcount = _linepoints.length();
			JSONArray columns;
			for (int ordernum = 0; ordernum < pointcount; ordernum++) {
				columns = _linepoints.getJSONArray(ordernum);
				ps.clearParameters();
				//lineid, ordernum, longitude,latitude,utcdate,altitude
				ps.setLong(1, lineid);
				ps.setLong(2, ordernum);
				ps.setDouble(3, columns.getDouble(0));
				ps.setDouble(4, columns.getDouble(1));
				ps.setTimestamp(5, Timestamp.valueOf(columns.getString(2)));
				ps.setDouble(6, columns.getDouble(3));
				ps.execute();
			}
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			_error = e.getMessage();
			return false;
		}
	}
	

	@Override
	public void handleCancellation() {
		// TODO Auto-generated method stub

	}

}
