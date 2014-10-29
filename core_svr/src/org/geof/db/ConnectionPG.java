package org.geof.db;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.geof.log.GLogger;

/**
 * Class inherits from the DBConnection abstract object for connecting to a Postgresql database
 * 
 * @author Jeff Boehmer
 * @comanpay  Ft Collins Research, LLC.
 * @url	www.ftcollinsresearch.org
 *
 */

public class ConnectionPG extends DBConnection {

	public String error;
	private boolean _hasPGeometry = false;
	
	@Override
	public boolean connect(String connstr, String uid, String pwd) {
		_connstr = connstr;
		_uid = uid;
		_pwd = pwd;
		return connect();
	}
	
	/**
	 * Establishes a connection to the Postgresql database
	 * @return True if connection succeeded, false if error occured.
	 */
	private boolean connect() {
		boolean rtn = false;
		try {
			if (!disconnect()) {
				GLogger.error("ConnectionPG: disconnect failed");
				return rtn;
			}
			
			if (!_driverLoaded) {
				try {
			        Class.forName("org.postgresql.Driver");
			        _driverLoaded = true;
		        } catch (Exception e) {
		        	GLogger.error(e);
		        	return rtn;
		        }
			}
			
			//jdbc:postgresql://servername/database
			//jdbc:postgresql://host:port/database
			_conn = DriverManager.getConnection(_connstr, _uid, _pwd);
			if (_conn == null) {
				GLogger.error("ConnectionPG.connect _conn == null");
				GLogger.error("_connstr: " + _connstr + ", _uid: " + _uid + ", _pwd: " +  _pwd);
				return rtn;
			}
			_conn.setAutoCommit(_autoCommit);
			rtn = true;

		} catch (SQLException se) {
			GLogger.error("ConnectionPG.connect failed: " + se.getMessage());
			error = "connect: " + se.getMessage();
		} catch (Exception e) {
			GLogger.error("ConnectionPG.connect failed: " + e.getMessage());
			error = "connect: " + e.getMessage();
		}
		return rtn;
	}

	@Override
	public String to_Date(java.sql.Date date) {
		return "TO_DATE('yyyy-mm-dd','" + date.toString() + "')";
	}

	@Override
	public String to_date(java.util.Date date) {
		return to_date(new java.sql.Date(date.getTime()));
	}
	
	/**
	 * Enables spatial query abilities by adding the Postgis PGgeometry class to the Postgresql connection object. 
	 */
	public void addPGeometry() {
		try {
			if (_hasPGeometry) return;
			((org.postgresql.PGConnection )_conn).addDataType("geometry", org.postgis.PGgeometry.class);
			_hasPGeometry = true;
		} catch (SQLException e) {
			GLogger.error(e);
		}

	}
}
