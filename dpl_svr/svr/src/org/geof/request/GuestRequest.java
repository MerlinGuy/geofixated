package org.geof.request;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.util.HashMap;

import org.geof.dpl.GuestObject;
import org.geof.dpl.mgr.DomainMgr;
import org.geof.prop.GlobalProp;
import org.geof.request.DBRequest;
import org.geof.service.AuthorityMgr;
import org.geof.db.DBInteract;
import org.geof.db.ParameterList;
import org.json.JSONArray;
import org.json.JSONObject;


public class GuestRequest extends DBRequest {

	public final static String TYPE = "type";	
	public final static String STATUS = "status";
	public final static String DOMAIN = "domain";
		
	private final static String _sqlRead = "SELECT d.*, ud.usrid FROM domain d LEFT OUTER JOIN usr_domain ud ON (d.id = ud.domainid)";
	private final static String _sqlReadOwned = "SELECT d.* FROM domain d, usr_domain ud WHERE d.id = ud.domainid AND ud.usrid=?";
	private final static String _sqlReadAllOwned = "SELECT d.* FROM domain d, usr_domain ud WHERE d.id = ud.domainid";
	private final static String _sqlUsrDomain = "SELECT d.* FROM domain d, usr_domain ud WHERE d.id=? AND ud.usrid = ? AND d.id = ud.domainid";

	@Override
	public String getEntityName() {
		return "domain";
	}

	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} 
		else if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".owned")) {
			return readOwned();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".ping")) {
			return readPing();
		} 
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} 
		else {
			return false;
		}
	}

	@Override
	/**
	 * 	
	 */
	public boolean create() {
		GuestObject oGuest = null;
		
		try {
			_data.put("session", _session);
			DomainMgr dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError(dm.getError());
			}			
			oGuest = new GuestObject(dm, _dbi, _data);
			GlobalProp.getExecutor().execute(oGuest);
//			oGuest.initialize( dm, _dbi, _data );
//			oGuest.create();
			return true;
			
		} catch (Exception e) {
			if (oGuest != null) {	
				oGuest._tstate.logError(e.getMessage());
				oGuest.backout(oGuest.dirName);
			}
			return setError("GuestRequest.createGuest failed: "+ e.getMessage());
			
		} finally {
			
			if (oGuest != null) {
				oGuest.cleanUp ();
			}
		}
	}
		
	@Override	
	/**
	 * 
	 */
	public boolean read() {
		DomainMgr dm = null;
		HashMap<String, JSONObject> mapDom = new HashMap<String, JSONObject>();
		boolean canAdmin = false; 
		
		try {
			if (_session != null) {
				canAdmin = AuthorityMgr.hasPermission(_session, "domain" , DELETE);
			}

			JSONArray domains = _dbi.readAsJson( _sqlRead, (ParameterList)null);
			long usrid = _session == null ? -1 :_session.getUsrID();
			JSONObject jo;
			int count = domains.length();
			String[] names = new String[count];
			for (int indx =0; indx < domains.length(); indx++) {
				jo = domains.getJSONObject(indx);
				jo.put(TYPE, 0);
				names[indx] = jo.optString(NAME);
				if (jo.optLong("usrid") == usrid) {
					jo.put(NAME, "Your Demo #" + indx);
				} else if ( !canAdmin ) {
					jo.put("usrid", -1);
					jo.put(NAME, "Demo #" + indx);					
				}
				mapDom.put(names[indx], jo);
			}
			domains = new JSONArray();
			
			dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError("GuestRequest.read : Could not instanciate DomainMgr");
			}
			boolean rtn = false;
			domains =  dm.getDomainsAsJSONArray();
			
			String name;
			JSONObject domain;
			for (int indx =0; indx < domains.length(); indx++) {
				jo = domains.getJSONObject(indx);
				name = jo.optString(NAME);
				if ( mapDom.containsKey(name)) {
					domain = mapDom.get(name);
					domain.put(TYPE, 2);
					domain.put(STATUS, jo.getInt(STATUS));
				} else {
					jo.put(TYPE,1);
					mapDom.put(name, jo);
				}
			}
			domains = new JSONArray();
			for (String key : mapDom.keySet()) {
				domains.put(mapDom.get(key));
			}
			_jwriter.writeArray(DATA, domains);
			return rtn;
			
		} catch (Exception e) {
			return setError("GuestRequest.read: " + e.getMessage() );
		}
		finally {
			if (dm != null) {
				dm.dispose();
			}
		}
	}
	
	public boolean readOwned() {
		try {
			ParameterList pl = new ParameterList();
			pl.add("usrid", _session.getUsrID());
			JSONArray domains = _dbi.readAsJson(_sqlReadOwned, pl);
			_jwriter.writeArray(DATA, domains);
			_dbi.closeAll();
			return true;
		} catch (Exception e) {
			return setError("GuestRequest.readOwned: " + e.getMessage() );
		}
	}
	
	public boolean readPing() {
		try {
			String domain = _where.optString(DOMAIN);
			JSONObject joMacip = MacipRequest.getDomain(_dbi, domain);
			JSONArray rows = new JSONArray();
			JSONObject data = new JSONObject();
			boolean reachable = false;
			if (joMacip != null) {
				String ip = joMacip.optString("ipaddress");
				if (ip != null && ip.length() > 6) {
					reachable = InetAddress.getByName(ip).isReachable(1000);
				}
			}
			data.put("reachable", reachable);
			rows.put(data);
			_jwriter.writeArray(DATA, rows);
			return true;

		} catch (Exception e) {
			return setError("GuestRequest.readPing: " + e.getMessage() );
		}
	}
	
	public static JSONArray getOwned(DBInteract dbi) throws Exception {
		return dbi.readAsJson(_sqlReadAllOwned, (ParameterList)null);
	}
	
	@Override	
	/**
	 * 
	 */
	public boolean delete() {
		try {

			long domain_id = _where.optLong(ID,-1);
			if (domain_id == -1) {
				return setError("GuestRequest.delete is missing id");
			}
			
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlUsrDomain);
			ps.setLong(1, domain_id);
			ps.setLong(2, _session.getUsrID());
			if (! _dbi.executeLR(_sqlUsrDomain, ps)) {
				return setError(_dbi.getError());
			}
			
			JSONObject jo = _dbi.getLastReturning();
			if (jo == null) {
				return setError("GuestRequest.delete Delete failed - not owner of " + domain_id);
			} 
			
			String domain_name = jo.optString(NAME,null);
			if (domain_name == null) {
				return setError("GuestRequest.delete domain not found for id: " + domain_id);
			}
			
			DomainMgr dm = new DomainMgr(_fields.optString("qemu_svr", null));
			if (dm.hasError()) {
				return setError("GuestRequest.delete failed to instantiate DomainMgr");
			}			
			String host_img_dir = _fields.optString("host_img_dir", null);
			DomainRequest.delete ( dm, _dbi, domain_name, host_img_dir);
			dm.dispose();
			return true;
		} catch (Exception e) {
			return setError("GuestRequest.delete Failed: "	+ e.getMessage());
		}
	}
	
}
