package org.geof.request;

import org.bouncycastle.util.encoders.Hex;
import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.email.EmailSender;
import org.geof.encrypt.CipherMgr;
import org.geof.prop.GlobalProp;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.KeyPair;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

public class RsaencryptionRequest extends DBRequest {

	public final static String ENTITYNAME = "rsaencryption";
	
	public final static String FIELDLIST = "id,modulus,exponent";
	
	private final static String _sqlClearcode = "SELECT email FROM usr WHERE clearcode=?";
//	private final static String _sqlCreateRsa = "INSERT INTO rsaencryption (modulus,exponent,pexponent,p,q,dp,dq,qinv) VALUES (?,?,?,?,?,?,?,?)";
	
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return super.read();
		} else if (_actionKey.equalsIgnoreCase(READ + ".rsaencryption")) {
			return readEncryption();
		} else if (_actionKey.equalsIgnoreCase(READ + ".rsaemail")) {
			return emailRsa();
		} else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} else if (_actionKey.equalsIgnoreCase(CREATE + ".aeskey")) {
 			return createAesKey();
		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return true;
		} else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return super.delete();
		} else if (_actionKey.equalsIgnoreCase(EXECUTE + ".rsaemail")) {
			return emailUsrRsa();
		} else {
			return super.process();
		}
	}

	@Override
	public boolean create() {
		try {
			return createPublicKey(_dbi) != null;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	public boolean readEncryption() {
		try {
			JSONArray pkis = _dbi.readAsJson(_entitymap, JsonUtil.getDataRead(FIELDLIST, null, "id,createdate") );
			int choice = (int)(Math.random() * pkis.length());
			JSONObject pkey = pkis.getJSONObject(choice);
			_jwriter.openArray(DATA);
			_jwriter.writeValue(pkey);
			_jwriter.closeArray();
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	public boolean emailRsa() {
		try {
			String email = _session.getData(UsrRequest.EMAIL);	
			this.sendRsaEmail(email);
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	public boolean emailUsrRsa() {
		try {
			long usrid = _where.optLong("usrid");
			String email = UsrRequest.getEmail(_dbi, usrid, null);
			this.sendRsaEmail(email);
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}
	
	public static String getPKI(DBInteract dbi) throws Exception {
		JSONObject jPKIs = JsonUtil.getDataRead(FIELDLIST, null, "id,createdate");
		EntityMap emap = EntityMapMgr.getEMap(RsaencryptionRequest.ENTITYNAME);
		JSONArray pkis = dbi.readAsJson(emap, jPKIs );
		int choice = (int)(Math.random() * pkis.length());
		JSONObject pkey = pkis.getJSONObject(choice);		
		return pkey.toString();
	}
	
	private void sendRsaEmail(String email_address) throws Exception{
		String pkey = getPKI(_dbi);
		EmailSender.sendEmail( email_address, "message from Geofixated", pkey);
	}

	public static void emailRsa(DBInteract dbi, String clearcode) throws Exception {		
		PreparedStatement ps = dbi.getPreparedStatement(_sqlClearcode);
		ps.setString(1, clearcode);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			String email_address = rs.getString(1);
			rs.close();
			EntityMapMgr.initialize( dbi );
			String pkey = getPKI(dbi);

			EmailSender.sendEmail( email_address, "message from Geofixated", pkey);
		} else {
			throw new Exception("Clearcode not found");
		}
	}

	public static boolean emailRsaByLogin(DBInteract dbi, String loginname) {
		boolean rtn = false;
		try {
			
			String email = UsrRequest.getEmail(dbi, null, loginname);
			if (email != null) {
				EntityMapMgr.initialize( dbi );
				EntityMap emap = EntityMapMgr.getEMap("rsaencryption");
				JSONArray pkis = dbi.readAsJson(emap, JsonUtil.getDataRead(FIELDLIST, null, "id,createdate") );
				int choice = (int)(Math.random() * pkis.length());
				JSONObject pkey = pkis.getJSONObject(choice);

				String mail_login = GlobalProp.getProperty("mail_login");
				String mail_pwd = GlobalProp.getProperty("mail_pwd");
				String service_name = "smtp." + GlobalProp.getProperty("service_name");
				HashMap<String,String> mapEmail = new HashMap<String,String>();
				mapEmail.put(EmailSender.HOST,service_name);
				mapEmail.put(EmailSender.TO_ADDR,email);
				mapEmail.put(EmailSender.LOGIN, mail_login);
				mapEmail.put(EmailSender.PWD,mail_pwd);
				mapEmail.put(EmailSender.SUBJECT,"message from geofixated");
				mapEmail.put(EmailSender.MESSAGE,pkey.toString());
				EmailSender.send(mapEmail);
				rtn = true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtn;
	}
	
	public static KeyPair createPublicKey(DBInteract dbi) {
		return CipherMgr.createRsaKeyPair(true, dbi);
	}

	private boolean createAesKey() {
		try {
			String hexAesKey = _data.getString("aeskey");
			_session.setAesKeySpec( Hex.decode(hexAesKey) );

			boolean success = _session.getAesKeySpec() != null;
			_jwriter.openArray(DATA);
			_jwriter.openObject();
			_jwriter.writePair("success", success);
			_jwriter.closeObject();
			_jwriter.closeArray();

			return success;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}
	
	
}
