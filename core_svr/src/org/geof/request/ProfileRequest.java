package org.geof.request;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.geof.service.SessionMgr;
import org.geof.util.JsonUtil;
import org.json.JSONObject;

public class ProfileRequest extends DBRequest {

	public final static String SETTINGS = "settings";
	public final static String TOKENS = "tokens";	

	public String getEntityName() {
		return "usr";
	}

	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		}
		else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		}
		else if (_actionKey.equalsIgnoreCase(UPDATE + ".password")) {
			return change_password();
		}
		else if (_actionKey.equalsIgnoreCase(UPDATE + ".settings")) {
			return updateSettings();
		}
		else if (_actionKey.equalsIgnoreCase(CREATE)) {				
			return setError("User does not have permission to create a profile");
		}
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return setError("User does not have permission to create a profile");
		} 
		else if (_actionKey.equalsIgnoreCase(UPDATE + ".tokens")) {
			return updateTokens();
		}
		else {
			return false;
		}
	}
	
	@Override
	public boolean read() {
		try {
			_where.put(ID,_session.getUsrID());
			int rtn = UsrRequest.checkUsr(READ,_session, _where);
			if (UsrRequest.IS_USR == rtn) {
				JsonUtil.remove(_data, COLUMNS, UsrRequest.RTN_BANNED);
				return super.read();
			} else if (UsrRequest.ID_MISSING == rtn) {
				setError("Data.Fields missing ID");
			} else {
				setError("User doesn't have permission to read other users.");
			} 
		} catch (Exception e) {
			setError(e.getMessage());
		}
		return false;
	}

	private boolean updateTokens() {
		try {
			List<?> tokens = SessionMgr.updateTokens(_session);
			_jwriter.openArray(DATA);
			_jwriter.openObject();
			_jwriter.writePair(TOKENS, StringUtils.join(tokens, ","));
			_jwriter.closeObject();
			_jwriter.closeArray();
			return true;
		} catch (Exception e) {
			return setError(e);
		}
	}
	
	public boolean change_password() {
		
		try {
			UsrRequest.change_password(
					_dbi, 
					_session.getUsrID(), 
					_fields.optString( UsrRequest.PASSWORD, null), 
					_fields.optString(UsrRequest.CLEARCODE, "")
					);
			return true;
			
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}

	public boolean changeEmail(HashMap<String,String> values) {
		try {		
			String email = values.get(UsrRequest.EMAIL);
			if (email == null || email.length() < 8) {
				setError("Invalid email address");
				return false;
			}
			
			String loginname = _session.getLoginname();
			if (loginname == null ||  UsrRequest.loginExists(_dbi, loginname)) {
				return setError("Invalid loginname");
			}
			PreparedStatement ps = _dbi.getPreparedStatement(UsrRequest.SqlUpdEmail);
			ps.setString(1, email);
			ps.setString(2, loginname);
			ps.execute();
			ps.close();
			return true;
		} catch (Exception e) {
			return setError(e.getMessage());
		}
	}		

	@Override
	public boolean update() {
		try {
			JsonUtil.remove(_data, FIELDS, UsrRequest.RTN_BANNED);
			JSONObject fields = _data.getJSONObject(FIELDS);
			UsrRequest.updateUsr(_dbi, _session.getUsrID(), fields);
			return true;
		} catch (Exception e) {
			return setError(e);
		}
	}
	
	public boolean updateSettings() {
		try {
			JSONObject data = JsonUtil.getFieldsWhere (SETTINGS, _fields.optString(SETTINGS, ""), ID, _session.getUsrID());
			return _dbi.update(_entitymap, data);
		} catch (Exception e) {
			return setError(e);
		}
	}
}