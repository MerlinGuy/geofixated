package org.geof.service;

import org.geof.db.DBConnMgr;
import org.geof.db.DBInteract;
import org.geof.db.EntityMapMgr;
import org.geof.dpl.mgr.GuestMgr;
import org.geof.encrypt.CipherMgr;
//import org.geof.job.TaskExecutor;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.VersionRequest;
import org.geof.store.StorageMgr;
import org.geof.util.FileUtil;
import org.geof.service.SessionMgr;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

/**
 * The DoppleContextListener is responsible for performing all system initialization before the
 * application's Servlet becomes availabe.  
 * 
 * It loads the initialization properties from files and the database.
 * It initializes the following systems
 * <ul>
 * 	<li>Logging system</li>
 * 	<li>Database Connection Pool</li>
 * 	<li>EntityMap Manager</li>
 * 	<li>Global Properties</li>
 * 	<li>Session Manager</li>
 * 	<li>Authority Manager</li>
 * </ul>
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.geofixated.com
 * 
 */
public class DoppleContextListener implements ServletContextListener {

	public static final String LOGLEVEL = "loglevel";
	public static long applicationInitialized = 0L;
	public static GlobalProp _gprops = null;
	public static String CONF_PATH = "";
	
	private static final boolean DEBUG = false;

	/**
	 * Application Startup Event
	 * Override of the ServletContextListener method which is called prior to the WebServlet startup.
	 * @param ce ServletContextEvent
	 */
	@Override
	public void contextInitialized(ServletContextEvent ce) {
		try {
			if (DEBUG) {
				Thread.sleep(10000);
			}
			initialize(ce);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void initialize(ServletContextEvent ce) {
		
		// Read in the intial bootstrap properties from file
		ServletContext sc = ce.getServletContext();
		String basePath = sc.getRealPath("/") + "WEB-INF/";
		String initlogpath =  basePath + "logs/init.log";		
		String geoflogpath = basePath + "logs/dopple.log";
		CONF_PATH =  basePath + "conf/";
		String confPath = CONF_PATH + "file.conf";
		String proppath = CONF_PATH + "dopple.conf";
		
		GLogger.initialize(geoflogpath, initlogpath, "Application started at ",  GLogger._VERBOSE);
		GLogger.setAppend(false);
		GLogger.writeInit("*** Logger.initialized ");

		_gprops = GlobalProp.instance();
		GLogger.writeTime( _gprops.getBool("write_time", false) );
		
		// Read in the server configs from the dopple.conf file
		_gprops.populate(proppath);		
		_gprops.put("basepath", basePath, false);		
		
		// Read in all the database properties
		DBInteract dbi = new DBInteract(DBConnMgr.getConnection(), null);
		GLogger.writeInitTime();
		_gprops.populate(dbi);

		// Print out version information
		GLogger.writeInit("  ");
		GLogger.writeInit(" [ Server Software Version: " + VersionRequest.getVersion() + " ]");
		GLogger.writeInit("  ");
		GLogger.writeInitTime();
		
		// setup filetypes
		FileUtil.loadFileTypes(confPath);

		EntityMapMgr.initialize(dbi);
		GLogger.writeInitTime();

		_gprops.printToInitLog();
		GLogger.writeInitTime();
		
		GLogger.writeInit("");
		if (_gprops.hasKey(LOGLEVEL)) {
			GLogger.setLogLevel(_gprops.getInt(LOGLEVEL, GLogger._VERBOSE));
			GLogger.verbose("  --- Logger log level reset to : " + _gprops.get(LOGLEVEL));
		}

		boolean rtn = StorageMgr.instance().initialize(dbi);
		if (rtn) {
			GLogger.writeInit("  ---  Storage Location count: " + StorageMgr.instance().getSize());
		} else {
			GLogger.writeInit("????? StorageManager.initialize Failed ");
		}
		GLogger.writeInitTime();

		Long sessiontimeout = null;
		if (_gprops.hasKey("sessiontimeout")) {
			sessiontimeout = Long.valueOf(_gprops.get("sessiontimeout"));
		}
		SessionMgr.initialize(sessiontimeout);
		GLogger.writeInitTime();
		
		CipherMgr.initialize();
		GLogger.writeInitTime();
		
		boolean Securityoff = _gprops.getBool("securityoff", false);
		boolean ReadOnlySecurity = _gprops.getBool("readonlysecurity", false);
		AuthorityMgr.initialize(dbi, Securityoff,ReadOnlySecurity);
		GLogger.writeInitTime();

		dbi.releaseConnection();
		GLogger.writeInit("  --- Context DB connection released\n");
		
		GLogger.writeInit("*** Setting up Transaction State Manager ");
		TStateMgr.initialize();
		GLogger.writeInitTime();
		
		GLogger.writeInit("*** Setting up GuestMgr ");
		GuestMgr.initialize();
		GLogger.writeInitTime();
		
		GLogger.writeInit("\nDoppleContextListener initialization complete.");
		GLogger.writeInitTime();
		GLogger.writeInit("------------------------------------------------\n");			
	}

	/**
	 * Application Shutdown Event
	 * This method is called just prior to the Java Servlet shutdown and is used to clean up objects in memory 
	 * @param ce
	 */
	public void contextDestroyed(ServletContextEvent ce) {
		DBConnMgr.shutdown();
	}
	
}

