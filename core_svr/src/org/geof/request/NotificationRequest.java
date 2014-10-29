	package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.geof.db.ColumnTrio;
import org.json.JSONException;



/**
 * Derived Request class is used to send messages to and from users via their sessionid
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */

public class NotificationRequest extends DBRequest {

	public final static String ENTITYNAME = "notification";
	public final static String NOTIFICATIONID = "notificationid";
	public final static String USRID = "usrid";
	
	private final static String _sqlReadNewFrom = "SELECT n.*, u.firstname, u.lastname FROM notification n, usr_notification un, usr u"
			+ " WHERE un.readdate is null AND un.usrid = ? AND n.usrid = u.id AND n.id = un.notificationid"
			+ " ORDER BY createdate desc";
	private final static String _sqlReadFrom = " FROM notification n, usr_notification un WHERE un.usrid = ? AND n.id = un.notificationid";
	private final static String _sqlReadDetail = "SELECT n.*, u.firstname, u.lastname FROM notification n, usr_notification un, usr u WHERE un.usrid = ? AND un.readdate is null AND n.usrid = u.id AND n.id = un.notificationid";
	private final static String _sqlUpdateRead = "UPDATE usr_notification SET readdate = ? WHERE usrid = ? and notificationid = ?";
	
	/**
	 * Overrides the base class process() to provide Session specific action handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(READ + ".new")) {
			return readNew();
		}  else if (_actionKey.equalsIgnoreCase(READ + ".detail")) {
			return readDetail();
		} else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		}  else if (_actionKey.equalsIgnoreCase(UPDATE +".read")) {
			return updateRead();
		} else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} 
		return false;
	}

	@Override
	protected boolean create() {
		try {
			_fields.put("userid", _session.getUsrID());
		} catch (JSONException e) {
			setError("NotificationRequest.create: " + e.getMessage());
		}
		return super.create();
	}
	
	@Override
	protected boolean read() {
		try {
			//todo: check in test for sql injection in the columns
			String columns = null;
			String orderby = "";
			String groupby = "";
			if (_data.has(COLUMNS)) {
				columns = _data.getString(COLUMNS);
				if (_data.has(ORDERBY)) {
					orderby = " ORDER BY " + _data.getString(ORDERBY);
				}
				if (_data.has(GROUPBY)) {
					groupby = " GROUP BY " + _data.getString(GROUPBY);
				}
			} else {
				columns = "n.*,un.readdate";
				orderby = " ORDER BY createdate";
			}
			String sql = "SELECT " + columns + _sqlReadFrom + groupby + orderby;
			PreparedStatement ps = _dbi.getPreparedStatement(sql);
			ps.setLong(1, _session.getUsrID());
			ResultSet rs = ps.executeQuery();
			_jwriter.writeResultset(DATA, rs, ColumnTrio.getColumns(rs.getMetaData()));
			ps.close();
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}
	
	protected boolean readNew() {
		try {
			String sql = _sqlReadNewFrom;
			PreparedStatement ps = _dbi.getPreparedStatement(sql);
			ps.setLong(1, _session.getUsrID());
			ResultSet rs = ps.executeQuery();
			_jwriter.writeResultset(DATA, rs, ColumnTrio.getColumns(rs.getMetaData()));
			ps.close();
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	protected boolean readDetail() {
		try {
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlReadDetail);
			ps.setLong(1, _session.getUsrID());
			ResultSet rs = ps.executeQuery();
			_jwriter.writeResultset(DATA, rs, ColumnTrio.getColumns(rs.getMetaData()));
			ps.close();
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	protected boolean updateRead() {
		try {
			if (! _where.has(NOTIFICATIONID)){
				setError("Notification update missing notificationid");
				return false;
			}			
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlUpdateRead);
			ps.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setLong(2, _session.getUsrID());
			ps.setInt(3,_where.getInt(NOTIFICATIONID));
			ps.execute();
			ps.close();
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}


	@Override
	protected boolean update() {
		// notifications are not updateable at this time.
		return false;
	}

	@Override
	protected boolean delete() {
		return super.delete();
	}

}
