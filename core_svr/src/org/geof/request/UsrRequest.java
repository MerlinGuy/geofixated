package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import org.geof.db.DBInteract;
import org.geof.db.EntityMapMgr;
import org.geof.db.ParameterList;
import org.geof.encrypt.EncryptUtil;
import org.geof.log.GLogger;
import org.geof.service.AuthorityMgr;
import org.geof.service.GSession;
import org.geof.service.SessionMgr;
import org.geof.util.JsonUtil;
import org.json.JSONObject;

public class UsrRequest extends DBRequest {
	public final static String ENTITYNAME = "usr";

	public final static String DIGEST = "digest";
	public final static String SALT = "salt";
	public final static String CLEARCODE = "clearcode";
	public final static String PASSWORD = "password";
	public final static String EMAIL = "email";
	public final static String NEWPASSWORD = "newpassword";
	public final static String LOGINNAME = "loginname";
	public final static String FIRSTNAME = "firstname";
	public final static String LASTNAME = "lastname";
	public final static String ATTEMPTS = "attempts";
	public final static String LASTATTEMPT = "lastattempt";
	public final static String ADMIN_BANNED = "lastattempt,digest,";
	public final static String STATUSID = "statusid";

	public final static String USR_REQD = "firstname,lastname,loginname,password,email";
	public final static String USR_BANNED = "statusid,attempts,digest,clearcode,salt,statusid";
	public final static String RTN_BANNED = "salt,attempts,digest,clearcode";

	public final static int IS_USR = 1;
	public final static int IS_ADMIN = 2;
	public final static int ID_MISSING = 3;
	public final static int NO_PERMISSION = 4;
	
	public final static String SqlUpdDigest = "UPDATE usr set digest=?,salt=?,clearcode=? WHERE loginname=?";
	public final static String SqlUpdEmail = "UPDATE usr set email=? WHERE loginname=?";
	public final static String SqlDigest = "UPDATE usr set digest=?,salt=?,clearcode=? WHERE id=?";

	private final static String _sqlExists = "SELECT count(id) as id_count FROM usr WHERE loginname = ?";
	private final static String _sqlEmail = "SELECT email FROM usr WHERE id = ?";
	private final static String _sqlEmailByLogin = "SELECT email FROM usr WHERE loginname = ?";
//	private final static String _sqlCreateUsr = "INSERT INTO usr (firstname,lastname,email,loginname,clearcode,digest,salt,statusid) VALUES (?,?,?,?,?,?,?,?) RETURNING id";

	private final static String[] updFields = {"firstname","lastname","loginname","password","email","initials","notes","statusid","addr1","addr2","city","state","zipcode","company"};

	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		}else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		}else if (_actionKey.equalsIgnoreCase(UPDATE + ".password")) {
			return change_password();
		}else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		}else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} else {
			return super.process();
		}
	}
	
	@Override
	public boolean create() {
		try {
			createUsr(_dbi, _fields);
			return true;
		} catch (Exception e){
			return setError(e.getMessage());
		}
	}
		
	public static void createUsr(DBInteract dbi, JSONObject fields) throws Exception {
		
		String[] reqd = USR_REQD.split(",");
		for (String key : reqd) {
			if (! fields.has(key)) {
				throw new Exception( "UsrRequest.createUsr missing value for " + key);
			}
		}
		String loginname = fields.getString(LOGINNAME);
		if (loginExists(dbi, loginname) ) {
			throw new Exception( "Invalid Login" );
		}
		String email = fields.getString(EMAIL); 
		if (email == null || email.length() < 3) {
			throw new Exception( "UsrRequest.createUsr invalid email address");
		}
		
		String password = fields.getString(PASSWORD);
		JSONObject ds = EncryptUtil.getDigestSalt(password);
		fields.put(DIGEST, ds.getString(DIGEST));
		fields.put(SALT, ds.getString(SALT));
		fields.put(CLEARCODE, EncryptUtil.hash256(email));
		dbi.create(EntityMapMgr.getEMap(UsrRequest.ENTITYNAME), JsonUtil.getDataFields(fields));
		if (dbi.hasError()) {
			throw new Exception(  dbi.getError() );
		}
	}
	
	@Override
	public boolean read() {
		try {
			int rtn = checkUsr(READ,_session, _where);
			if ((IS_USR == rtn) || (IS_ADMIN == rtn)) {
				JsonUtil.remove(_data, COLUMNS, RTN_BANNED);
				return super.read();
			} else if (ID_MISSING == rtn) {
				setError("Data.Fields missing ID");
			} else {
				setError("User doesn't have permission to read other users.");
			} 
		} catch (Exception e) {
			setError(e.getMessage());
		}
		return false;
	}

	@Override
	public boolean update() {
		try {
			int rtn = checkUsr(UPDATE,_session, _where);
			if (IS_USR == rtn)  {
				JsonUtil.remove(_data, FIELDS, RTN_BANNED);
				updateUsr(_dbi, _session.getUsrID(), _fields);
				return true;
			} else if (IS_ADMIN == rtn) {
				JsonUtil.remove(_data, FIELDS, ADMIN_BANNED);
				updateUsr(_dbi, _fields.getLong(ID), _fields);
				return true;
			} else if (ID_MISSING == rtn) {
				setError("Data.Fields missing ID");
			} else {
				setError("User doesn't have permission to update other users.");
			} 
			return false;
			
		} catch (Exception e) {
			GLogger.error(e);
			return false;
		}
	}
		
	public static void updateUsr(DBInteract dbi, Long id, JSONObject fields) throws Exception {
		
		JSONObject data = JsonUtil.getDataWhere(ID, id);
		JSONObject flds = data.has(FIELDS) ? data.getJSONObject(FIELDS) : new JSONObject();
		
		String email = fields.optString(EMAIL,null); 
		if (email != null) {
			flds.put(CLEARCODE, EncryptUtil.hash256(email));
			flds.put(EMAIL, email);
		}
		
		String password = fields.optString(PASSWORD, "");
		if ( password.length() > 0) {
			String error = isValidPassword(password);
			if (error != null) {
				throw new Exception(error);
			}
			JSONObject ds = EncryptUtil.getDigestSalt(password);
			flds.put(DIGEST, ds.getString(DIGEST));
			flds.put(SALT, ds.getString(SALT));
		}
		for (String fldName : updFields) {
			if (fields.has(fldName)){
				flds.put(fldName, fields.get(fldName));
			}
		}
		data.put(FIELDS, flds);
		dbi.update(EntityMapMgr.getEMap(UsrRequest.ENTITYNAME), data);
	}
	
	@Override
	public boolean delete() {
		long usrid = _where.optLong(ID, -1);
		if (usrid == -1) {
			return setError("Missing id for usr");
		}
		boolean rtn = super.delete();
		SessionMgr.coalesce(_dbi);
		return rtn;
	}
	
	public boolean changeEmail(HashMap<String,Object> values) {
		try {		
			String[] reqd = "loginname,email".split(",");
			for (String key : reqd) {
				if (! values.containsKey(key)) {
					setError("UsrRequest.changeEmail missing value for " + key);
					return false;
				}
				String value = (String)values.get(key);
				if (value == null || value.length() == 0) {
					setError("UsrRequest.changeEmail invalid value for " + key);
					return false;
				}
			}

			String email = (String)values.get(EMAIL);
			if (email == null || email.length() < 8) {
				setError("Invalid email address");
				return false;
			}
			
			String loginname = (String)values.get(LOGINNAME);
			if (loginname == null || loginExists(_dbi, loginname)) {
				setError("Invalid loginname");
				return false;
			}
			PreparedStatement ps = _dbi.getPreparedStatement(SqlUpdEmail);
			ps.setString(1, email);
			ps.setString(2, loginname);
			ps.execute();
			ps.close();
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}
	
	public static String getEmail(DBInteract dbi, Long usrid, String loginname) throws Exception {
		ResultSet rs = null;
		String error = null;
		String email = null;
		try {
			PreparedStatement ps = null;
			if (usrid != null) {
				ps = dbi.getPreparedStatement(_sqlEmail);
				ps.setLong(1, usrid);
				
			} else if (loginname != null) {
				ps = dbi.getPreparedStatement(_sqlEmailByLogin);
				ps.setString(1, loginname);
				
			} else {
				error = "UsrRequest.getEmail: missing both usrid and loginname";
				throw new Exception(error);
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				email = rs.getString(1);
			} else {
				error = "ERROR user not found";
			}
		} catch (Exception e) {
			error = "ERROR UsrRequest.getEmail: " + e.getMessage();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}
		}
		if (error != null) {
			throw new Exception(error);
		}
		return email;
	}
	
	
	public static boolean loginExists(DBInteract dbi, String loginname) throws Exception {
		ParameterList pl = new ParameterList(ENTITYNAME);
		pl.add("loginname", loginname);
		ResultSet rs = dbi.getResultSet(_sqlExists, pl);
		rs.next();
		boolean LoginExists = rs.getInt(1) > 0;
		rs.close();
		return LoginExists;
	}

	public boolean change_password() {
		try {			
			int rtn = checkUsr(UPDATE,_session, _where);
			if (ID_MISSING == rtn) {
				return setError("Data.Fields missing ID");
			} else if (NO_PERMISSION == rtn){
				return setError("User doesn't have permission to update other users.");
			} 
			Long id = _where.getLong(ID);
			
			UsrRequest.change_password(
					_dbi, 
					id, 
					_fields.optString( PASSWORD, null), 
					_fields.optString(CLEARCODE, "")
			);
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}

	public static void change_password(DBInteract dbi, Long id, String password, String clearcode) throws Exception {
		try {			
			String error = UsrRequest.isValidPassword(password);
			if (error != null) {
				throw new Exception(error);
			}			
			JSONObject flds = EncryptUtil.getDigestSalt(password);
			flds.put(CLEARCODE, clearcode == null ? "" : clearcode);
			JSONObject data = JsonUtil.getDataWhere(ID, id);
			data.put(FIELDS, flds);
			dbi.update(EntityMapMgr.getEMap(UsrRequest.ENTITYNAME), data);
			error = dbi.getError();
			if (error != null) {
				throw new Exception(error);
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public static String isValidPassword(String password) {
		if ((password == null) || (password.length() == 0)) {
			return "Empty password";
		}

		if (password.length() < 8) {
			return "Short password";
		}

		int strength = 0;
		String[] checks = { 
				".*[a-z]+.*", // lower
				".*[A-Z]+.*", // upper
				".*[\\d]+.*", // digits
				".*[@#$%]+.*" // symbols
		};

		if (password.matches(checks[0])) {
			strength += 25;
		}
		if (password.matches(checks[1])) {
			strength += 25;
		}
		if (password.matches(checks[2])) {
			strength += 25;
		}
		if (password.matches(checks[3])) {
			strength += 25;
		}
		if (strength < 75) {
			return "Weak password - Must match 3 of (lowercase, uppercase, number, symbol)";
		}
		return null;
	}

	public static int checkUsr(String action, GSession session, JSONObject where) {
		int id = where.optInt( ID, -1);		
		if (id == session.getUsrID()) {
			return IS_USR;
		} 
		else if ( AuthorityMgr.hasPermission(session, ENTITYNAME, CREATE)) {
			return IS_ADMIN;
		}
		else if (id == -1) {
			return ID_MISSING;
		} 
		return NO_PERMISSION;		
	}
}