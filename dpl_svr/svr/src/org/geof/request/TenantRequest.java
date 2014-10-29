package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.geof.db.DBInteract;
import org.geof.db.EntityMapMgr;
import org.geof.email.EmailSender;
import org.geof.prop.GlobalProp;
import org.geof.util.JsonUtil;
import org.json.JSONObject;


public class TenantRequest extends DBRequest {
	
	private static final String _sqlUsrUgroup = "INSERT INTO usr_ugroup (usrid, ugroupid) VALUES (?,?)";
//	private static final String _sqlReadUsr = "SELECT u.* FROM usr u INNER JOIN usr_domain ud ON u.id = ud.usrid WHERE u.id=?";
	private static final String _sqlDomainOwner = "SELECT count(u.id) FROM usr u, usr_domain ud WHERE u.id=? AND u.id = ud.usrid ";

	public static final long DEMO_GROUP_ID = 4;
	public static final String EMAIL = "email";
	
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} 
		else if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} 
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} 
		else {
			return false;
		}
	}

	@Override
	public String getEntityName() {
		return "usr";
	}
	
	@Override
	public boolean create() {
		long usrid = -1;
		try {
			_fields.put(UsrRequest.STATUSID, SessionRequest.DISABLED);
			UsrRequest.createUsr(_dbi, _fields);
			JSONObject lr = _dbi.getLastReturning();
			usrid = lr.getLong(ID);
			PreparedStatement ps = _dbi.getPreparedStatement( _sqlUsrUgroup );
			ps.setLong(1, usrid);
			ps.setLong(2, DEMO_GROUP_ID);
			ps.execute();
			
			String address = _fields.getString(EMAIL);
			String subject = "Geofixated registration confirmation";
			String pki = RsaencryptionRequest.getPKI(_dbi);
			EmailSender.sendEmail( address, subject, pki);
			
			String emailGeofizated = GlobalProp.getProperty("tenant_notify_address");
			subject = "Geofixated registration request";
			
			String fname = _fields.optString(UsrRequest.FIRSTNAME);
			String lname = _fields.optString(UsrRequest.LASTNAME);
			String email = _fields.optString(UsrRequest.EMAIL);
			
			String message = fname + " " + lname + " at " + email + " has requested a registration.";

			EmailSender.sendEmail( emailGeofizated, subject, message );
			return true;
		} catch (Exception e) {
			delete(_dbi, usrid);
			return setError(e.getMessage());
		} finally{
			_dbi.closeAll();
		}
	}
	
	protected static boolean delete(DBInteract dbi, long usrid) {		
		return dbi.delete(EntityMapMgr.getEMap("usr"), JsonUtil.getWhere(ID, usrid));
	}
	
	public boolean delete() {
		try {
			Long usrid = _where.optLong(ID, -1);
			if (usrid == -1) {
				return setError("WHERE statement missing id");
			}
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlDomainOwner);
			ps.setLong(1, _where.optLong(ID, -1));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				if (count == 0) {
					return super.delete();
				} else {
					return setError("Can not delete user with active demo");
				}
			}
		} catch (SQLException e) {
			return setError(e.getMessage());
		} finally {
			_dbi.closeAll();
		}
		return false;
	}
	
}
