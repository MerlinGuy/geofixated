package org.geof.dpl.mgr;

import java.sql.Timestamp;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.libvirt.DomainInfo;

import org.geof.db.DBInteract;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.GuestRequest;

public class GuestMgr implements Runnable {

	public static final String GM_INTERVAL = "gm_interval";
	public static final String GM_POOL_SIZE = "gm_pool_size";
	public static final String GM_INITIAL_DELAY = "gm_initial_delay";
	public final static String STARTDATE = "startdate";
	public final static String ENDDATE = "enddate";
	public final static String NAME = "name";
	public final static String RUNNABLE = "runnable";
	
	protected DomainInfo.DomainState RUNNING = DomainInfo.DomainState.VIR_DOMAIN_RUNNING;

	protected int _interval = 30000; // Check each 30 seconds.
	protected int _initial_delay = 10000; // Time to wait before the first run

	protected Thread _thread = null;
	protected boolean _alive = false;
	protected JSONObject _jsonConfig = null;
	protected String _name = null;
	protected boolean _paused = false;
	protected Date _lastRun = null;
	protected DomainMgr _domMgr = null;
	
	private int _pool_size = 4;
//	private ScheduledThreadPoolExecutor _stpExecutor = null;
	
	private static GuestMgr _instance = null;
	
	
	public GuestMgr() {
		GlobalProp gprop = GlobalProp.instance();
		_interval = gprop.getInt(GM_INTERVAL, _interval);
		_pool_size = gprop.getInt(GM_POOL_SIZE, _pool_size);
		_initial_delay = gprop.getInt(GM_INITIAL_DELAY, _initial_delay);

//		_stpExecutor = new ScheduledThreadPoolExecutor(_pool_size);
		_domMgr = new DomainMgr();
	}
	
	public static void initialize() {
		try {
			if (_instance == null) {
				_instance = new GuestMgr();
				GlobalProp.getExecutor().execute(_instance);
				GLogger.writeInit("*** GuestMgr.initialized");
			}
		} catch (Exception e) {
			GLogger.error(e);
		}
	}

	@Override
	public void run() {
		try {
			Thread.sleep(_initial_delay);
		} catch (InterruptedException e) {
		}
		
		GLogger.verbose("  --- Started GuestMgr");
		_alive = true;
		while (_alive) {
			if (!_paused) {
				_lastRun = new Date();
			}
			
			processDomains();
			
			try {
				Thread.sleep(_interval);
			} catch (InterruptedException e) {}
		}
	}
	
	private void processDomains() {
		DBInteract dbi = null;
		try {
			dbi = new DBInteract();
			JSONArray domains = GuestRequest.getOwned(dbi);
			if (domains.length()== 0) {
				return;
			}
			
			JSONObject joDomain;
			String name;
			Timestamp startdate, enddate;
			Date date = new Date();
			DomainInfo.DomainState dstate = null;
			
			int count = domains.length();
			for (int indx = 0; indx < count; indx++) {
				joDomain = domains.getJSONObject(indx);
				name = joDomain.getString(NAME);
				dstate = _domMgr.getDomainState(name);
				
				if (joDomain.optBoolean(RUNNABLE, false)){			
					startdate = (Timestamp) joDomain.opt(STARTDATE);
					if (startdate != null) {
						enddate = (Timestamp) joDomain.opt(ENDDATE);
						if (dstate == RUNNING ) {
							if (startdate.after(date) || (enddate != null && enddate.before(date))) {
								_domMgr.shutdown(name);
							}
						} else {
							if (startdate.before(date) && (enddate == null || enddate.after(date))) {
								_domMgr.start(name);
							}
						}
					}
					
				} else if ( dstate == RUNNING ){
					_domMgr.shutdown(name);
				}
			}
		} catch (Exception e) {
			GLogger.error("GuestMgr.getDomains: " + e.getMessage());
		} finally {
			if (dbi != null) {
				dbi.dispose();
			}
		}
	}
	
	public boolean paused() {
		return _paused;
	}

	public void setPaused(boolean paused) {
		_paused = paused;
	}
	
	public long getLastRun() {
		return _lastRun.getTime();
	}
	
	/**
	 * 
	 */
	public void dispose() {
		this._alive = false;
		if (this._thread != null) {
			GLogger.debug("Thread ended for: " + _thread.getName());
			this._thread.interrupt();
//			if (this._stpExecutor != null) {
//				this._stpExecutor.shutdownNow();
//			}
		}
	}


}
