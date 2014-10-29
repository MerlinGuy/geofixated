package org.geof.dpl;

import org.geof.db.DBInteract;
import org.geof.db.ParameterList;
import org.geof.log.GLogger;
import org.geof.request.DBRequest;
import org.json.JSONArray;
import org.json.JSONObject;

public class DemoBuildplan {
	
	private final static String _sqlUnsetDemo = "TRUNCATE TABLE demo_buildplan";
	private final static String _sqlSetDemo = "INSERT INTO demo_buildplan (buildplanid) VALUES (?)";
	private final static String _sqlRead = "SELECT dbp.buildplanid, bp.name, bp.domainname FROM demo_buildplan dbp, buildplan bp WHERE dbp.buildplanid = bp.id";
	

	public static JSONArray read(DBInteract dbi) throws Exception {
		return dbi.readAsJson(_sqlRead, (ParameterList)null);
	}
		
	public static boolean setIsDemo(DBInteract dbi, JSONObject data) {
		try {
			GLogger.verbose("DemoBuildplan.setIsDemo");
			JSONObject flds = data.optJSONObject(DBRequest.FIELDS);
			
			if (flds != null) {
				long id = flds.optLong(DBRequest.ID,-1);
				if (id > -1) {
					boolean isDemo = flds.optBoolean("isDemo", false);
					GLogger.verbose("DemoBuildplan.setIsDemo has buildplanid: " + id);
					dbi.executePL(_sqlUnsetDemo, null);
					if (isDemo) {
						GLogger.verbose("DemoBuildplan.setIsDemo add demo_buildplan record: " + id);
						ParameterList pl = new ParameterList();
						pl.add("buildplanid", id);
						return dbi.executePL(_sqlSetDemo, pl);
					}
				}
			}
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			return false;
		}
	}
	
	public static JSONObject getDemoBuildplan(DBInteract dbi) throws Exception {
		JSONArray ja = dbi.readAsJson(_sqlRead, (ParameterList)null);
		return (ja == null) ? null : ja.optJSONObject(0);
	}
}
