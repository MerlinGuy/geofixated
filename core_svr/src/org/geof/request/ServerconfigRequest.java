package org.geof.request;

import java.sql.ResultSet;

import org.geof.db.DBConnMgr;
import org.geof.db.DBConnection;
import org.geof.db.DBInteract;
import org.geof.db.ParameterList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Derived DBRequest class to handle database interactions for the Serverconfig table. The
 * Java Servlet which makes up the server side of the Omosis application is initialized from
 * thres sources. The first two are files used during startup to initialize the Servlet. The
 * third is the Serverconfig table which holds global values used by all users accessing the
 * system.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class ServerconfigRequest extends DBRequest {

	private static final String _sqlSelect = "SELECT value FROM serverconfig WHERE name = ?";
	private static final String _InfoName = "('defaultextent','searchmedia')";
	private static final String _sqlInSelect = "SELECT name, value FROM serverconfig WHERE name IN " + _InfoName;

	
	/**
	 * Overrides the base class process() to provide Serverconfig specific action handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return super.read();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".info")) {
			return info();
		} 
		else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} 
		else {
			return super.process();
		}
	}

	/**
	 * Creates new Name/Value pairs in the Serverconfig table of the database.
	 * 
	 * @return Returns true if the method succeeds otherwise false if an error occurs
	 */
	@Override
	protected boolean create() {
		if (super.delete()) {
			return super.create();
		}
		return false;
	}
	
	protected boolean info() {
		try {
			ResultSet rs = _dbi.getPreparedStatement(_sqlInSelect).executeQuery();
			_jwriter.writeResultset(DATA, rs, "name,value".split(","));
			return true;
		} catch (Exception e) {
			return setError(e);
		}
	}

	protected JSONArray info(DBConnection conn) {
		try {
			ResultSet rs = conn.getResultset(_sqlInSelect);
			JSONArray ja = new JSONArray();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				row.put(rs.getString(1), rs.getString(2));
				ja.put(row);
			}
			return ja;
		} catch (Exception e) {
			setError(e);
		}
		return null;
	}


	public String get(String key){
		String value = null;
		DBConnection conn = DBConnMgr.getConnection();
		try {
			ParameterList pl = new ParameterList(getEntityName());
			pl.add("name",key);
			ResultSet rs = (new DBInteract(conn, null)).getResultSet(_sqlSelect, pl);
			if (rs != null){
				if (rs.next()) {
					value = rs.getString(1);
				}
				rs.close();
			}
			
		} catch (Exception e) {
			setError(e);
		} finally {
			DBConnMgr.release(conn);
		}
		return value;
	}

}
