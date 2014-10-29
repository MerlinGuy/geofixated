package org.geof.admin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import org.geof.db.DBConnMgr;
import org.geof.db.DBConnection;
import org.geof.db.DBInteract;
import org.geof.db.EntityMapMgr;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.EntityRequest;
import org.geof.request.RsaencryptionRequest;
import org.geof.request.UsrRequest;
import org.geof.service.JsonWriter;

public class GeofAdmin {
	
	private static GlobalProp _gprops = null;
	private DBInteract _dbi = null;
	private static final boolean _debug = false;
	private static int _DatabaseType = DBConnMgr.POSTGRESQL;

	private static final Map<String, Object> cmdMap;
    static {
        Map<String, Object> aMap = new HashMap<String,Object>();
        aMap.put("-a", "action");
        aMap.put("-c", "config");
        aMap.put("-d", "debug");
        aMap.put("-l", "log");
        aMap.put("-f", "firstname");
        aMap.put("-n", "lastname");
        aMap.put("-u", "loginname");
        aMap.put("-p", "password");
        aMap.put("-e", "email");
        cmdMap = Collections.unmodifiableMap(aMap);
    }
	public GeofAdmin() {}
	
	public static void main(String [] args) {
		try {
			if (_debug) {
//				Thread.sleep(10000);
				System.out.println("Parameters");
				for (int i=0;i<args.length;i++) {
					System.out.println(args[i]);
				}
			}
			HashMap<String, Object> mapArgs = new HashMap<String, Object>();
			
			int len = args.length;
			if (len == 1) {
				args = args[0].split(" ");
			}
			len = args.length;
			if (len % 2 == 1) {
				System.out.println("Incorrect number of arguements: " + len);
				outputHelp();
			}
			String cmd,key;
			
			int indx = 0;
			while (indx < len) {
				cmd = args[indx];
				if (cmd.startsWith("-")) {
					key = cmdMap.containsKey(cmd) ? (String)cmdMap.get(cmd) : cmd.substring(1);
					mapArgs.put(key, args[indx+1]);
					indx += 2;
				} else {
					outputHelp();
				}
			}
			
			GeofAdmin ga = new GeofAdmin();
			String logPath = (String)mapArgs.get("log");
			if (logPath != null) {
				ga.setupLog(logPath);
			} else {
				GLogger.initialize(null, null, "GeofAdmin.main started: ", GLogger._VERBOSE);
			}
			
			String action = (String)mapArgs.get("action");
			boolean rtn = false;
			
			GLogger.debug("Running " + action);
			
			ga.init((String)mapArgs.get("config"));

			if (action.equals("fixEntity") ) {
				rtn = ga.fixEntities(); 
			}
			else if (action.equals("updatePwd")) {			
				rtn = ga.updatePassword(mapArgs);
			}
			else if (action.equals("updateEmail")) {			
				rtn = ga.updateEmail(mapArgs);
			}
			else if (action.equals("createUsr")) {			
				rtn = ga.createUser(mapArgs);
			}
			else if (action.equals("emailRsa")) {			
				rtn = ga.emailRsa(mapArgs);
			}
			else if (action.equals("createRsa")) {			
				rtn = ga.createRsa(mapArgs);
			}
			else {
				outputHelp();
			}				
			if (rtn) {
				GLogger.debug("... completed successfully");
			} else {
				GLogger.debug("... failed to complete");
			}
			GLogger.debug("Finished.");
		} catch (Exception e) {
			GLogger.error(e.getMessage());
			System.exit(1);
		}

	}

	public void init(String confPath) throws Exception {
		String basePath = null; 
		if (confPath == null) {
			basePath = System.getProperty("user.dir");
			confPath = basePath + "/../conf/geof.conf";
		}
		
		File confFile = new File(confPath);
		if ( ! confFile.exists()) {
			// try absolute path 
			confPath = "/var/lib/tomcat6/webapps/geof/WEB-INF/conf/geof.conf";
			confFile = new File(confPath);
			if ( ! confFile.exists()) {
				throw new Exception("geof.conf file missing - " + confFile.getCanonicalPath());
			}
		}
		
		_gprops = GlobalProp.instance();
		if (! _gprops.populate(confFile)) {
			throw new Exception("Global Properties initialization failed");
		}
		
		_gprops.put("basepath", basePath, false);
		
		String connStr = _gprops.get("connstr");
		String uid = _gprops.get("uid");
		String pwd = _gprops.get("pwd");

		DBConnection conn = openConnection(connStr, uid, pwd);
		if (conn == null) {
			throw new Exception("Could not connect to database - " + connStr);
		}
		JsonWriter null_writer = null;
		_dbi = new DBInteract( conn, null_writer );
		if (_dbi == null) {
			throw new Exception("Failed to instanciate DBInteract");
		}
	}
	
	public static DBConnection openConnection(String connStr, String uid, String pwd) {
		try {
			Class<?> c = Class.forName("org.geof.db." + DBConnMgr.CONN_NAMES[_DatabaseType]);
			DBConnection conn = (DBConnection) c.newInstance();

			if (conn.connect(connStr, uid, pwd)) {
				return conn;
			} else {
				GLogger.error("openConnection failed");
				GLogger.error("_connStr " + connStr);
				GLogger.error("_uid " + uid);
				GLogger.error("_pwd " + pwd);
			}

		} catch (Exception e) {
			GLogger.error("GeofAdmin.openConnection");
			GLogger.error(e);
		}
		return null;
	}

	public boolean fixEntities() {
		EntityMapMgr.initialize(_dbi);
		EntityRequest er = new EntityRequest();
		er.init(this._dbi);
		return er.fixEntities();
	}
	
	public boolean updatePassword(HashMap<String, Object> mapArgs) {
		String[] reqd = "loginname,password".split(",");
		for (String key : reqd) {
			if (! mapArgs.containsKey(key)) {
				GLogger.error("ERROR - updatePassword missing value for " + key);
				return false;
			}
		}
		UsrRequest ur = new UsrRequest();
		ur.init(this._dbi);
		boolean rtn = ur.updatePassword(mapArgs);
		String error = ur.getError();
		if ( error != null) {
			System.out.println(error);
		}
		return rtn;
	}
	
	public boolean emailRsa(HashMap<String, Object> mapArgs) {
		String[] reqd = {"loginname"};
		for (String key : reqd) {
			if (! mapArgs.containsKey(key)) {
				GLogger.error("ERROR - sendRSA missing value for " + key);
				return false;
			}
		}
		boolean rtn =  RsaencryptionRequest.emailRsaByLogin(_dbi, (String)mapArgs.get("loginname"));
		return rtn;
	}
	
	public boolean createRsa(HashMap<String, Object> mapArgs) {
		return  RsaencryptionRequest.createPublicKey(_dbi) != null;
	}
	
	public boolean updateEmail(HashMap<String, Object> mapArgs) {
		String[] reqd = "loginname,email".split(",");
		for (String key : reqd) {
			if (! mapArgs.containsKey(key)) {
				GLogger.error("ERROR - updateEmail missing value for " + key);
				return false;
			}
		}
		UsrRequest ur = new UsrRequest();
		ur.init(this._dbi);
		return ur.changeEmail(mapArgs);
	}
	
	public boolean createUser(HashMap<String, Object> mapArgs) {
		String[] reqd = "firstname,lastname,loginname,password,email".split(",");
		for (String key : reqd) {
			if (! mapArgs.containsKey(key)) {
				GLogger.error("ERROR - createUsr missing value for " + key);
				return false;
			}
		}		
		UsrRequest ur = new UsrRequest();
		ur.init(this._dbi);
		return ur.createUsr(mapArgs);
	}
	
	public static void outputHelp() {
		System.out.println("Command failed - Invalid arguement format\n");
		System.out.println("Valid commands...");
		System.out.println("    fixEntities\nOr");
		System.out.println("    updatePwd -u <username> -p <password>\nOr");
		System.out.println("    updateEmail -u <username> -e <email>\nOr");
		System.out.println("    createUsr -u <username> -p <pwd> -f <firstname> -l <lastname> -e <email>\nOr");
		System.out.println("    emailRsa -u <username>\nOr");
		System.out.println("    createRsa");
		System.exit(1);
	}
	
	public void setupLog(String logPath) {
		int loglevel = GLogger._VERBOSE;
		GLogger.initialize(logPath, null, "GeofAdmin run at ", loglevel);		
	}
	
}