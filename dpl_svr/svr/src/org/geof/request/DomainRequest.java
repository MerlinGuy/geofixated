package org.geof.request;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.db.ParameterList;
import org.geof.dpl.GuestObject;
import org.geof.dpl.DomainData;
import org.geof.dpl.ReturnObj;
import org.geof.dpl.mgr.DomainMgr;
import org.geof.email.EmailSender;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.DBRequest;
import org.geof.util.DateUtil;
import org.geof.util.FileUtil;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.libvirt.Domain;

public class DomainRequest extends DBRequest {

	public final static String ENTITYNAME = "domain";
	
	public final static String TYPE = "type";	
	public final static String STATUS = "status";
	public final static String ACTION = "action";
	public final static String STARTDATE = "startdate";
	public final static String ENDDATE = "enddate";

	public final static String IMAGE = "image";;
	
	public final static int FROM = 0;
	public final static int TO = 1;

	public final static int RUNNING = 1;
	public final static int OFFLINE = 5;
	
	public final static int START = 0;
	public final static int SHUTDOWN = 1;
	public final static int PAUSE = 2;
	public final static int DB = 0, HOST = 1, DB_HOST = 2;
		
	private final static String _sqlFullDetail = "SELECT * FROM Domain WHERE dirname=?";
	private final static String _sqlGeneric = "SELECT d.*, ud.usrid FROM domain d LEFT OUTER JOIN usr_domain ud ON (d.id = ud.domainid)";
	

	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} 
		else if (_actionKey.equalsIgnoreCase(CREATE +".guest")) {
			return createGuest();
		} 
		else if (_actionKey.equalsIgnoreCase(CREATE +".import")) {
			return importDomain();
		} 
		else if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".detail")) {
			return readDetail();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".generic")) {
			return readGeneric();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".usr")) {
			return readUsr();
		}
		else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		} 
		else if (_actionKey.equalsIgnoreCase(UPDATE + ".state")) {
			return updateState();
		} 
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} 
		else if (_actionKey.equalsIgnoreCase(DELETE+".sever")) {
			return severDomain(true);
		} 
		else {
			return super.process();
		}
	}

	
	public boolean createGuest() {
		GuestObject oGuest = null;
		
		try {
			DomainMgr dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError(dm.getError());
			}			
			_data.put("session", _session);
			oGuest = new GuestObject(dm, _dbi, _data);
			oGuest.initialize(dm, _dbi, _data);
			oGuest.create();
			return true;
			
		} catch (Exception e) {
			if (oGuest != null) {
				oGuest.backout(oGuest.dirName);
			}
			return setError("DomainRequest.createGuest failed: "+ e.getMessage());
			
		} finally {
			
			if (oGuest != null) {
				oGuest.cleanUp ();
			}
		}
	}
	
	public boolean importDomain() {
		try {
			String guest_name = _fields.optString(NAME);
			if (guest_name.length() == 0) {
				return setError("DomainRequest.import missing name field");
			}
			//			JSONObject joD = dd.toJSON();
			JSONObject data = new JSONObject();
			data.put(FIELDS, _fields);
			DomainRequest.register(_dbi, data, _data.optString("qemu_svr", null), null);
			return true;
			
		} catch (Exception e) {
			return setError("DomainRequest.importDomain: " + e.getMessage() );
		}
	}
		
	public static void register(DBInteract dbi, JSONObject data, String connect_str, DomainMgr dm) throws Exception {
		try {
			JSONObject fields = data.getJSONObject(FIELDS);
			String guest_name = fields.optString(DIRNAME);
			
			if (guest_name.length() == 0) {
				throw new Exception("Missing name field");
			}
			if (dm == null) {
				dm = new DomainMgr(connect_str);
			}
			if (dm.hasError()) {
				throw new Exception("Could not instanciate DomainMgr");
			} 
			// Changed from dm.getDomainData(dirName);
			DomainData dd = dm.getDomainData(guest_name);
			if (dd == null) {
				throw new Exception("Could not find domain: " + guest_name);
			}

			JSONObject flds = data.getJSONObject(FIELDS);
			flds.put(STATUS, 0);
			flds.put(TYPE, DB_HOST);
			flds.put(DIRNAME, fields.optString(DIRNAME));
			
			if (flds.has(STARTDATE)) {
				Date sdate = DateUtil.parse(flds.getString(STARTDATE), true);
				if (sdate != null) {
					flds.put(STARTDATE, DateUtil.getTimestamp(sdate.getTime()));
				} else {
					flds.remove(STARTDATE);
				}
			}
			if (flds.has(ENDDATE)) {
				Date edate = DateUtil.parse(flds.getString(ENDDATE), true);
				if (edate != null) {
					flds.put(ENDDATE, DateUtil.getTimestamp(edate.getTime()));
				} else {
					flds.remove(ENDDATE);
				}
			}
			EntityMap emap = EntityMapMgr.getEMap(ENTITYNAME);
			boolean rtn =  dbi.create(emap, data);
			
			if (! rtn ) {
				throw new Exception(dbi.getError());
			}
			
			JSONObject joIDom = dbi.getLastReturning();
			dd.domain_id = joIDom.getLong(ID);
			
//				1) create image
			JSONObject imgKeys = ImageRequest.createFromDomainData(dbi, dd);
			Long image_id = imgKeys.getLong(ID);
			
//				2) create files
			List <String> files = dd.files;
			JSONObject fData = JsonUtil.getDataFields();
			JSONObject joFlds = fData.getJSONObject(FIELDS);
			JSONObject fKeys = null;
			File file;
			for (int indx=0; indx < files.size(); indx++) {
				file = new File(files.get(indx));
				joFlds.put("filename", file.getName());
				joFlds.put("fileext", FileUtil.getExtension(file));
				joFlds.put("filesize", file.getTotalSpace());
				joFlds.put("originalname", file.getAbsolutePath());
				joFlds.put("filetype", -1);
				joFlds.put("status", 0);
				joFlds.put("storagelocid", -1);
				joFlds.put("viewid", -1);
				joFlds.put("geomtype", -1);
				fKeys = dbi.create("file",fData);
				
//					3) link files to image
				dbi.addLink("image", image_id, "file", fKeys.getLong(ID));
			}
			
//				4) update domain - set imageid = new image id
			Object[] dflds = new Object[] {"imageid",image_id};
			Object[] dWhere = new Object[] {"id",dd.domain_id};
			JSONObject dData = JsonUtil.getData(dflds, dWhere);
			rtn = dbi.update(emap, dData);

// 			5) link the domain to the user		
			dbi.addLink(ENTITYNAME, dd.domain_id, "usr", dbi.getUsrId());
			
			if (GlobalProp.instance().getBool("notify_guest_create", true)) {
				sendDomainEmail( data );
			}
			
		} catch (Exception e) {
			GLogger.error(e.getMessage());
			throw new Exception( e.getMessage() );			
		}
		finally {
			if (dm != null) {
				dm.dispose();
			}
		}
	}
	
	private static void sendDomainEmail(JSONObject data) throws Exception {
		JSONObject fields = data.getJSONObject(Request.FIELDS);
		String address = fields.getString(EmailSender.EMAIL);
		String subject = "Your new domain has been registered";
		String[] regexs = {"firstname","lastname","url","loginname","pwd"};
		String msg = "Hello %firstname %lastname,"
				+ "\r\n\r\n Your new domain has been registered and will be avaiable after %startdate."
				+ "\r\n\r\n The URL for your domain is %url."
				+ "\r\n The login is %loginname with password %pwd"
				+ "\r\n\r\n Thank you for using the Dopple Demo Server,\r\n\r\n\t\t\t Geofixated.org.";
		
		for (String rplmnt : regexs) {
			msg = msg.replaceAll("%" + rplmnt, fields.optString(rplmnt,""));
		}
		if (fields.has(STARTDATE)) {
			msg = msg.replaceAll("%startdate", DateUtil.parse(fields.getString(STARTDATE), true).toString());
		} else {
			msg = msg.replaceAll("%startdate", "(no start date)");
		}
		EmailSender.sendEmail( address, subject, msg);		
	}
		
	public boolean severDomain(boolean missingIsError){
		try {
			severDomain(_dbi, _data.getJSONObject(WHERE), missingIsError);
			return true;
		} catch (Exception e) {
			return setError("DomainRequest.severDomain: " + e.getMessage());
		}
	}
	
	public static void severDomain(DBInteract dbi, JSONObject where, boolean missingIsError) throws Exception {
		EntityMap emap = EntityMapMgr.getEMap(ENTITYNAME);
		JSONObject data = new JSONObject();
		data.put(WHERE, where);
		JSONArray jaDomain = dbi.readAsJson(emap, data);
		int count = jaDomain.length();
		if (count == 0 && missingIsError) {
			throw new Exception("Domain does not exist in database");
		} else if (count > 2) {
			throw new Exception("Where constraint brought back multiple domains");
		}
		
		dbi.delete(emap, where);
		
		dbi.deleteLinks(ENTITYNAME, where.optLong(ID));
		
		if ( count > 0) {
			JSONObject joDomain = jaDomain.getJSONObject(0);
			long image_id = joDomain.optLong("imageid", -1);
			if (image_id > -1) {
				ImageRequest.deleteDb(dbi, JsonUtil.getWhere(ID, image_id));
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
		try {
			JSONArray domains = _dbi.readAsJson( _entitymap, _data);
			JSONObject jo;
			for (int indx =0; indx < domains.length(); indx++) {
				jo = domains.getJSONObject(indx);
				jo.put(TYPE, 0);
				mapDom.put(jo.optString(NAME), jo);
			}
			domains = new JSONArray();
			
			dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError("DomainRequest.read : Could not instanciate DomainMgr");
			}
			boolean rtn = false;
			if (_where != null && _where.names() != null) {
				if (_where.has(ID)) {
					domains.put(dm.getDomainById( _where.optInt(ID) ));
				} else if (_where.has(DIRNAME)) {
					domains.put(dm.getDomain( _where.optString(DIRNAME) ));					
				} else if (_where.has(STATUS)) {
					domains = dm.getDomainsByState( _where.optInt(STATUS));
				}
			} else {
				domains =  dm.getDomainsAsJSONArray(); 
			}
			
			String name;
			JSONObject domain;
			for (int indx =0; indx < domains.length(); indx++) {
				jo = domains.getJSONObject(indx);
				// cindy .. NAME vs DIRNAME?
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
			return setError("DomainRequest.read: " + e.getMessage() );
		}
		finally {
			if (dm != null) {
				dm.dispose();
			}
		}
	}
	
	public boolean readDetail() {
		try {
			if ( ! _data.has(WHERE)) {
				return setError("DomainRequest.readFullDetail: Where section is missing");
			}
	
			if ( ! _where.has(DIRNAME)) {
				return setError("DomainRequest.readFullDetail: Where section dirname missing ");
			}
			
			JSONObject jo = null;
			
			ParameterList pl = new ParameterList(_entitymap, _where);
			JSONArray ja = _dbi.readAsJson( _sqlFullDetail, pl);
			
			jo = (ja.length() == 0) ? new JSONObject() : ja.getJSONObject(0);

			int imageid = jo.optInt("imageid", -1);
			JSONArray jaImages = null;
			if (imageid != -1) {
//					GLogger.verbose("using domain imageid");
				jaImages = ImageRequest.getFilesJson(_dbi, imageid);
				jo.put("images", jaImages);
			} else {
				long buildplanid = jo.optLong("buildplanid", -1);
				if (buildplanid != -1) {
//						GLogger.verbose("using buildplan imageid");
					jaImages = BuildplanRequest.getImageFilesJson(_dbi, buildplanid);
					jo.put("images", jaImages);
				} else {
					GLogger.error("no imageid found");
					jo.put("images", new JSONArray());
				}
			}				

			String dirname = _where.optString(DIRNAME);
			jo.put(DIRNAME, dirname);
			DomainMgr dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError("DomainRequest.read : Could not instanciate DomainMgr");
			}
			Domain domain = dm.getDomain(dirname);
			if (domain != null) {
				DomainData dd = dm.getDomainData(domain);
				jo.put(STATUS, dd.domain.getInfo().state.ordinal());
				JsonUtil.copyTo(dd.toJSON(), jo, true);
			}
			_jwriter.writePair(DATA, jo);
			return true;
			
		} catch (Exception e) {
			return setError("DomainRequest.readFullDetail: " + e.getMessage() );
		}
	}
	
	public boolean readUsr() {
		try {
			JSONObject data = JsonUtil.getDataWhere("usrid", _session.getUsrID());
			 _dbi.read(EntityMapMgr.getEMap("usr_domain"),data);
			 return true;
		} catch (Exception e) {
			return setError(e.getMessage());
		}
	}
		
	public boolean readGeneric() {
		DomainMgr dm = null;
		HashMap<String, JSONObject> mapDom = new HashMap<String, JSONObject>();
		try {
			JSONArray domains = _dbi.readAsJson( _sqlGeneric, (ParameterList)null);
			JSONObject jo;
			int count = domains.length();
			String[] names = new String[count];
			for (int indx =0; indx < domains.length(); indx++) {
				jo = domains.getJSONObject(indx);
				jo.put(TYPE, 0);
				names[indx] = jo.optString(DIRNAME);
				mapDom.put(names[indx], jo);
				jo.put(SessionRequest.USRID, jo.optInt(SessionRequest.USRID, -1));
			}
			domains = new JSONArray();
			
			dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError("DomainRequest.read : Could not instanciate DomainMgr");
			}
			boolean rtn = false;
			domains =  dm.getDomainsAsJSONArray();
			
			String name;
			JSONObject domain;
			for (int indx =0; indx < domains.length(); indx++) {
				jo = domains.getJSONObject(indx);
				// Keep as NAME : affects DB and HOST listing
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
			return setError("DomainRequest.read: " + e.getMessage() );
		}
		finally {
			if (dm != null) {
				dm.dispose();
			}
		}
	}
	
	public boolean updateState() {
		try {
			String domainName = _where.optString(DIRNAME, null);
			int action = _fields.optInt(ACTION, -1);
			String connect_string = _data.optString("qemu_svr", null);
			
			String error = DomainRequest.updateState( domainName, action, connect_string);
			if (error != null && error.length() > 0) {
				return setError(error);
			}
			return true;

		} catch (Exception e) {
			return setError ("DomainRequest.updateState -  " + e.getMessage());
		}			
	}
	
	public static String updateState(String domainName, int action, String connect_string) {
		try {
			
			if (domainName == null || domainName.length() == 0) {
				return "DomainRequest.updateState - domainName is empty";
			}

			DomainMgr dm = new DomainMgr(connect_string);
			if (dm.hasError()) {
				return "DomainRequest.updateState : Could not instanciate DomainMgr";
			}
			HashMap<String, Domain> domains = dm.getAllDomains();
			Domain domain = domains.get(domainName);
			if (domain == null) {
				return "DomainRequest.updateState - Domain named: " + domainName + " not listed on host.";
			}
			
			boolean rtn;
			switch (action) {
				case START:
					rtn = dm.start(domainName);
					break;
				case SHUTDOWN:
					rtn = dm.shutdown(domainName);
					break;
				case PAUSE:
					rtn = dm.shutdown(domainName);
					break;
				default:
					return "DomainRequest.updateState - Action (" + action + ") not valid";
			}
			if (! rtn ) {
				return dm.getError();
			}
			return "";

		} catch (Exception e) {
			return "DomainRequest.updateState -  " + e.getMessage();
		}			
	}
	
	@Override	
	/**
	 * 
	 */
	public boolean delete() {
		try {
			DomainMgr dm = new DomainMgr(_fields.optString("qemu_svr", null));
			if (dm.hasError()) {
				return false;
			}			

			if (dm.isRemoteSvr()) {
				String user = _fields.optString("user", null);
				String pwd = _fields.optString("pwd", null);
				
				if (user == null) {
					return setError("Missing -user parameter");
				}
				if (pwd == null) {
					return setError("Missing -pwd parameter");
				}
			}
			
			String domain_name = _where.optString("dirname", null);
			// 8/7 changed ,null to ,"/media/kvm/"
			String host_img_dir = _fields.optString("host_img_dir", "/media/kvm/");
			delete ( dm, _dbi, domain_name, host_img_dir);
			dm.dispose();
			return true;
		} catch (Exception e) {
			return setError("DomainRequest.delete Failed: "	+ e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param domain_name
	 * @param host
	 * @param user
	 * @param pwd
	 * @param host_img_dir
	 * @return
	 * @throws Exception 
	 */
	public static void delete(DomainMgr dm, DBInteract dbi, String domain_name, String host_img_dir) throws Exception {

		try {
			// Get the 'from' domain information (image files, xml, etc)
			if (domain_name == null) {
				throw new Exception ("Missing domain_name parameter");
			}
			
			EntityMap emapDomain = EntityMapMgr.getEMap(ENTITYNAME);
			JSONObject domainData = JsonUtil.getDataWhere(DIRNAME, domain_name);

			domainData.put(FIELDS, JsonUtil.getFields("runnable", false));
			dbi.update(emapDomain, domainData);

			// TODO: write return object for calls like this
			Domain domain = dm.getDomain(domain_name);
			DomainData dData = null;
			String domaindir = null;

			if (domain != null) {
				// Shutdown the domain if necessary
				if (dm.getDomainOrdinalState(domain_name) == RUNNING) {
					if (!dm.shutdown(domain_name)) {
						GLogger.debug("Error: cannot shutdown Domain: "	+ domain_name);
					}
				}
				dData = dm.getDomainData(domain_name);
				domaindir = dData.getDomainDirectory();
				if (!dm.undefine(domain_name)) {
					throw new Exception ("Failed to remove Domain: " + domain_name);
				}
			}
			
			JSONArray jaDomain = dbi.readAsJson( emapDomain, domainData);

			JSONObject joDomain = jaDomain.getJSONObject(0);
			long domainid = joDomain.getLong(ID);		
			
			JSONObject joWhere = domainData.getJSONObject(WHERE);
			dbi.delete(emapDomain, joWhere);
			severDomain(dbi, joWhere, false);
			MacipRequest.clearByDomain(dbi, domain_name);
			
			long imageid = joDomain.optLong("imageid", -1);
			if ( imageid > -1) {
				ImageRequest.deleteDbById(dbi, imageid);
			}			

			if (domaindir == null && host_img_dir != null) {
				domaindir = host_img_dir + domain_name + "/";
			}

			if (domaindir != null) {
				File dir = new File(domaindir);
				if (dir.isDirectory()) {
					try {
						FileUtils.deleteDirectory(dir);
					} catch (IOException e) {
						throw new Exception ("Failed to remove domain directory: "
								+ domaindir + e.getMessage());
					}
					if (dir.exists()) {
						throw new Exception ("Failed to remove domain directory: "	+ domaindir);
					}
				}
			}
			
			dbi.deleteLinks(ENTITYNAME, domainid);

		} catch (Exception e) {
			throw e;
		} finally {
			if (dm != null) {
				dm.dispose();
			}
		}

	}
	
	public static JSONArray getDomains(DBInteract dbi) throws Exception {
		return dbi.readAsJson( EntityMapMgr.getEMap(ENTITYNAME) , new JSONObject());
	}

	//TODO: Move this to ExecTool.java
	/**
	 * 
	 * @param cmd
	 * @return
	 */
	public ReturnObj exec(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			int exit_code = p.waitFor();
			String stdout = FileUtil.getStream(p.getInputStream());
			String stderr = FileUtil.getStream(p.getErrorStream());
			return new ReturnObj(exit_code == 0, stderr, stdout, exit_code);
		} catch (Exception e) {
			return ReturnObj.getError(e.getMessage());
		}
	}

}
