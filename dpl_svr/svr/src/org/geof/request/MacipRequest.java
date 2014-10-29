package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import org.geof.db.DBInteract;
import org.geof.db.EntityMapMgr;
import org.geof.log.GLogger;
import org.geof.request.DBRequest;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;


public class MacipRequest extends DBRequest {

	public final static String ENTITYNAME = "macip";
	
//	private final static String _sqlFree = "SELECT * FROM macip WHERE domainid IS NULL";
//	private final static String _sqlSetFree = "UPDATE macip set domain = null WHERE domainid IS ?";
	private final static String _sqlReserveRnd = "UPDATE macip SET domain=?,status=? WHERE id = (SELECT min(id) FROM macip WHERE domain IS NULL) returning id";
	private final static String _sqlReserve = "UPDATE macip SET domain=?,status=? WHERE id = ? AND domain IS NULL returning id";
	private final static String _sqlClear = "UPDATE macip SET domain=null,status=0 WHERE domain=?";
	private final static char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	
	public final static int FREE=0,IN_USE=1,RESERVED=2,OFFLINE=3;
	
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} 
		else if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} 
		else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		} 
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} 
		else {
			return super.process();
		}
	}
	
	public static boolean setDomain(DBInteract dbi, long id, String domain, Integer status) {
		Object[] fields = null;
		if (status != null) {
			fields = new Object[] {"domain",domain,"status",status};
		} else {
			fields = new Object[] {"domain",domain};
		}
		Object[] where = new Object[] {"id",id};		
		JSONObject data = JsonUtil.getData(fields, where);
		return dbi.update(EntityMapMgr.getEMap(ENTITYNAME), data);
	}
	
	public static JSONObject getDomain(DBInteract dbi, String domain) throws Exception {
		JSONObject where = JsonUtil.getWhere("domain",domain);
		JSONArray ja = dbi.readAsJson(EntityMapMgr.getEMap(ENTITYNAME), JsonUtil.getDataWhere( where));
		return (ja.length() == 0) ? null : ja.getJSONObject(0);
	}
	
//	public static long freeMacip(DBInteract dbi, String domain) {
//		long id = -1;
//		try {
//			ResultSet rs = dbi.getResultSet(_sqlSetFree, null);
//			id = rs.getLong(ID);
//			rs.close();
//			JSONObject data = JsonUtil.getData(new Object[] {"domain",domain}, new Object[] {ID,id});
//			if (! dbi.update(EntityMapMgr.getEMap(ENTITYNAME), data)) {
//				id = -1;
//			}
//		} catch (Exception e) {
//			GLogger.error(e);
//		}
//		return id;
//	}
	
	public static JSONObject reserve(DBInteract dbi, Long macipid, String domain) throws Exception {
		// create a random name for now
		if (domain == null) {
			StringBuilder sb = new StringBuilder();
			Random random = new Random();
			for (int i = 0; i < 20; i++) {
			    char c = chars[random.nextInt(chars.length)];
			    sb.append(c);
			}
			domain = sb.toString();
		}
		
		PreparedStatement ps = null;
		if (macipid == -1) {
			ps = dbi.getPreparedStatement(_sqlReserveRnd);
		} else {
			ps = dbi.getPreparedStatement(_sqlReserve);				
		}
		ps.setString(1, domain);
		ps.setLong(2, RESERVED);
		if (macipid != -1) {
			ps.setLong(3, macipid);
		}
		JSONArray recs = JsonUtil.toJSONArray( ps.executeQuery());
		ps.close();
		return (recs.length() == 0) ? null : getDomain(dbi, domain);
	}
	
	
	public static boolean clearByDomain(DBInteract dbi, String domain) {
		boolean rtn = false;
		if (domain != null && domain.length() > 0) {
			PreparedStatement ps = dbi.getPreparedStatement(_sqlClear);
			try {
				ps.setString(1, domain);
				ps.executeQuery();
				ps.close();
				rtn = true;
			} catch (SQLException e) {
				GLogger.error(e.getMessage());
			}
		}
		return rtn;
	}

}
