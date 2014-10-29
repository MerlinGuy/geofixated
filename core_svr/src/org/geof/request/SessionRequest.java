package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import org.geof.db.DBInteract;
import org.geof.db.EntityMapMgr;
import org.geof.encrypt.EncryptUtil;
import org.geof.service.AuthorityMgr;
import org.geof.service.SessionMgr;
import org.geof.store.StorageMgr;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Derived DBRequest class used to manage the active user Sessions. Sessions are
 * not stored in the database instead are an in memory list of Sessions.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */

public class SessionRequest extends Request {

	public final static String USRID = "usrid";
	public final static String USR = "usr";

	public final static String SESSIONID = "sessionid";
	public final static String LOGIN = "login";
	public final static String LIST = "list";
	public final static String ALL = "all";

	public final static int LOGINFAILED = -1;
	public final static int NORMAL = 1;
	public final static int ADJUSTED = 2;
	public final static int DISABLED = 3;
	public final static int LOCKEDOUT = 4;

	public final static long WAITTIME = 300000;
	public final static int MAXTRIES = 3;
	public final static String FIELDLIST = "id,loginname,digest,salt,statusid,attempts,lastattempt,firstname,lastname,email";
	public final String _sqlSetLastAttempt = "UPDATE usr SET lastattempt=CURRENT_TIMESTAMP WHERE loginname=?";
	public final String _sqlSetAttemptCount = "UPDATE usr SET attempts=? WHERE id=?";
	
	/**
	 * Overrides the base class process() to provide Session specific action
	 * handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(CREATE)) {
			boolean rtn = create();
			if (! rtn) {
				this._transaction.setTerminalRequestError();
			}
			return rtn;
		} else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		}
		return true;
	}

	/**
	 * Method checks the user login, password, and access status. If the user is
	 * valid with an active status and a correct pair of login and password then
	 * a session is created for the user and added to the SessionManager.
	 * 
	 * @return Returns true if no error occurs
	 */
	private boolean create() {

		try {
			if (_session != null) {
				SessionMgr.removeSession(_session.getKey());
				_session = null;
				_transaction.setSession(_session);
			}

			String loginname = _where.optString(UsrRequest.LOGINNAME, "");
			if ( loginname.length() == 0 ) {
				return setError("Loginname missing from login");
			}
			
			String sent_pwd = _where.optString(UsrRequest.PASSWORD, "");
			
			JSONObject where = new JSONObject();
			where.put(UsrRequest.LOGINNAME,loginname);
			_data.put(WHERE, where);
			_data.put(COLUMNS, FIELDLIST);
			
			JSONArray jaUsrs = _dbi.readAsJson( EntityMapMgr.getEMap(USR), _data );
			
			if (jaUsrs.length() == 0) {
				setLastAttempt(_dbi, loginname);
				return setError("User login failed");
			}

			JSONObject joUsr = jaUsrs.getJSONObject( 0 );
			
			String error = validate(joUsr, loginname, sent_pwd, _dbi);
			if (error != null) {
				return setError(error);
			}
			
			Long usr_id = joUsr.getLong(ID);
			String firstname = joUsr.getString(UsrRequest.FIRSTNAME);
			String lastname = joUsr.getString(UsrRequest.LASTNAME);
			String email = joUsr.getString(UsrRequest.EMAIL);

			ArrayList<Integer> ugroups = AuthorityMgr.getUGroups(_dbi, usr_id);
			if (_session == null) {
				_session = SessionMgr.createSession(usr_id, ugroups);
			}
			_session.setLoginname(loginname);
			_session.setData(UsrRequest.EMAIL, email);
			_transaction.setSession(_session);

			_jwriter.openObject(DATA);
			_jwriter.writePair(LOGIN, true);
			_jwriter.writePair("usrid", this._usrid);
			_jwriter.writePair("firstname", firstname);
			_jwriter.writePair("lastname", lastname);
			_jwriter.writePair("sessionid", _session.getSessionID());
			_jwriter.writePair("permissions", AuthorityMgr.convertPermissions(ugroups));
			_jwriter.writePair("storageloc", StorageMgr.instance().usableAsJson());
			_dbi.read("usrconfig", EntityMapMgr.getEMap("usrconfig"), JsonUtil.getDataWhere(this._usrid));
			_jwriter.closeObject();
			return true;
		} catch (Exception e) {
			return setError(e);
		}
	}

	/**
	 * Helper method that validates whether or not a user is allowed to
	 * created a session and if their login information is correct.
	 * 
	 * @return Returns an error if validation fails;
	 */
	private String validate(JSONObject joUsr, String loginname, String sent_pwd, DBInteract dbi) throws Exception {
	
		int statusid = joUsr.optInt("statusid", DISABLED);
	
		String error = null;
		// check for disbled login
		if (statusid == DISABLED || statusid == LOCKEDOUT) {
			error = "Your login is disabled. Please contact the adminstator.";
			
		} else {
			
			long usrid = joUsr.getLong(ID);
			String str_salt = joUsr.optString(UsrRequest.SALT,"");
					
			String digest1 = joUsr.optString(UsrRequest.DIGEST,"");
			String digest2 = EncryptUtil.getPasswordDigest(sent_pwd, str_salt);
			int attempts = joUsr.optInt("attempts",0);
			Timestamp lastAttempt = (Timestamp) joUsr.opt("lastattempt");
			
			long now = new Date().getTime();
			long waituntil = (lastAttempt == null) ? 0 : lastAttempt.getTime() + WAITTIME;
	
			if (waituntil < now) {
				attempts = 0;
			}
			
			if (attempts == MAXTRIES) {
				// too many tries
				int seconds = (int) ((waituntil - now) / 1000);
				int minutes = (int) (seconds / 60);
				seconds = seconds % 60;
				String strSecs = "0" + seconds;
				strSecs = strSecs.substring(strSecs.length() - 2);
				error = "User locked out for - " + minutes + ":" + strSecs;
				
			} else {
				if (digest1.equals(digest2)) {
					attempts = 0;
				} else {
					attempts++;
					error = "Login failed ";
				}
				PreparedStatement ps = dbi.getPreparedStatement(_sqlSetAttemptCount);
				ps.setInt(1,attempts);
				ps.setLong(2,usrid);
				ps.execute();
				ps.close();
			}
		}
		setLastAttempt(dbi, loginname);
		return error;
	}
	
	private void setLastAttempt(DBInteract dbi, String loginname) throws Exception {
		PreparedStatement ps = dbi.getPreparedStatement(_sqlSetLastAttempt);
		ps.setString(1,loginname);
		ps.execute();
	}
	
	/**
	 * This method keeps the Session alive.
	 * 
	 * @return Returns true if no error occurs
	 */
	private boolean update() {
		try {
			if (_session != null) {
				long usrid = _session.getUsrID();
				JSONObject result = new JSONObject();
				result.put("usrid", usrid);
				addData(result);
				return true;
			}
		} catch (Exception e) {
			return setError(e);
		}
		return setError(Request.NO_SESSION_ERROR);
	}

	/**
	 * Deletes the Session from the SessionManager's session list which
	 * effectively logs the user out of the system.
	 * 
	 * @return Returns true if no error occurs
	 */
	private boolean delete() {
		boolean rtn = false;
		try {
			if (_where != null) {
				if (_where.has(SESSIONID)) {
					String id = _where.getString(SESSIONID);
					if (id.equalsIgnoreCase(ALL)) {
						rtn = SessionMgr.removeAllSessions();
					} else {
						rtn = SessionMgr.removeSession(id);
					}
				}
			} else if (_session != null) {
				SessionMgr.removeSession(_session.getKey());
				_session = null;
				_dbi.clearSession();
			}
			rtn = true;
		} catch (Exception e) {
			setError(e);
		} finally {
			if (_transaction != null) {
				_transaction.setSession(_session);
			}
		}
		return rtn;
	}


	/**
	 * Reads all the session data and writes it to the response stream.
	 * 
	 * @return True if the method succeeds otherwise false if an error occurs.
	 */
	private boolean read() {
		boolean rtn = false;
		try {
			JSONArray sessionlist = SessionMgr.getSessionsAsJSON();
			_jwriter.writePair(DATA, sessionlist);
			rtn = true;
		} catch (Exception e) {
			setError(e);
		}
		return rtn;
	}

}
