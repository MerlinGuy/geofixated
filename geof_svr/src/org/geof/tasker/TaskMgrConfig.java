package org.geof.tasker;

import java.util.HashMap;

import org.geof.log.GLogger;
import org.geof.tasker.im.GIMTask;
import org.json.JSONArray;
//import org.json.JSONException;
import org.json.JSONObject;

public class TaskMgrConfig implements Runnable {

	public static final String FILE_NAME = "task_mgr.conf";

	public static final String TASKMANAGERS = "taskmanagers";
	public static final String IM_MANAGERS = "instant_messengers";
	public static final String NAME = "name";
	public static final String INTERVAL = "interval";
	public static final String MAX_PER_DAY = "max_per_day";
	public static final String INITIAL_DELAY = "initial_delay";
	public static final String TASK = "tasks";
	public static final String LOGIN = "login";
	public static final String PWD = "PWD";
	public static final String CLASS = "class";
	public static final String ENABLED = "enabled";

	private JSONObject _taskConfigs = new JSONObject();
	private static HashMap<String, ATaskManager> _mapMgrs = new HashMap <String, ATaskManager>();
	private static HashMap<String, GIMTask> _gimMgrs = new HashMap <String, GIMTask>();
	
	private static TaskMgrConfig _instance = null;
	protected static Thread _thread = null;
	protected String[] _enabled = null;

	/** The private constructor **/
	private TaskMgrConfig() {
//		String basePath = GlobalProp.instance().get("basepath");
//		String config_path = basePath + "conf/" + TaskMgrConfig.FILE_NAME;
//		this._taskConfigs = JsonUtil.parseConfigFile(config_path);
//		
//		boolean debug = JsonUtil.optBoolean(_taskConfigs, "debug", false);
//		this._enabled = JsonUtil.optString(_taskConfigs, "enabled", "").split(",");
//		GLogger.debug("TaskMgrConfig.debug " + debug);
//		if (debug) {
//			_thread = new Thread(this);
//			_thread.start();
//		} else {
//			this.buildTaskMgrs();
//		}
	}

	public static TaskMgrConfig getInstance() {
		if (_instance == null) {
			synchronized (TaskMgrConfig.class) {
				if (_instance == null) {
					_instance = new TaskMgrConfig();
				}
			}
		}
		return _instance;
	}

	public JSONObject getConfig(String key) {
		try {
			if (_taskConfigs.has(key)) {
				return this._taskConfigs.getJSONObject(key);
			}
		} catch (Exception e) {
			GLogger.error(e.getMessage());
		}
		return null;
	}

//	public boolean buildTaskMgrs() {
//		//TODO: automatically clear out old managers here
//		
//		_mapMgrs.clear();
//		_gimMgrs.clear();
//		try {
//			JSONArray jaRunList = this._taskConfigs.getJSONArray(RUN_LIST);
//			int len = jaRunList.length();
//			for (int rindx = 0; rindx < len; rindx++) {
//				JSONObject runItem = jaRunList.getJSONObject(rindx);
//				if (runItem.has(NAME)) {
//					String runtype = runItem.getString(NAME);
//					if (runtype.equalsIgnoreCase(TASKMANAGERS)) {
//						GLogger.debug("looking for Task Managers");
//						JSONArray jaTMgrs = this._taskConfigs.getJSONArray(TASKMANAGERS);
//						int count = jaTMgrs.length();
//						for (int indx = 0; indx < count; indx++) {
//							JSONObject joTMgr = jaTMgrs.getJSONObject(indx);
//							ATaskManager tm = this.instantiateTaskManager(joTMgr);
//							if (tm != null) {
//								_mapMgrs.put(tm.name(), tm);
//							}
//						}
//						
//					} else if (runtype.equalsIgnoreCase(IM_MANAGERS)) {
////						GLogger.debug("looking for GIM Managers");
//						JSONArray jaGIMgrs = this._taskConfigs.getJSONArray(IM_MANAGERS);
//						int count = jaGIMgrs.length();
//						JSONObject joImMgr;
//						for (int indx = 0; indx < count; indx++) {
//							joImMgr = jaGIMgrs.getJSONObject(indx);
//							GIMTask gim = this.instantiateGIMManager(joImMgr);
//							if (gim != null) {
//								_gimMgrs.put(gim.serviceName(), gim);
//							}
//						}						
//					}
//				}
//			}
//			
//			return true;
//		} catch (JSONException e) {
//			GLogger.error(e);
//			return false;
//		}
//	}

	public String getPkgName() {
		String name = this.getClass().getName();
		int indx = name.lastIndexOf('.');
		return name.substring(0, indx);
	}

	public ATaskManager instantiateTaskManager(JSONObject joConfig) {
		try {
			if (!joConfig.has(TaskMgrConfig.NAME)) {
				GLogger.error("TaskManager config is missing task name field");
			} else {
				String className = joConfig.getString(TaskMgrConfig.NAME);
				String fullClassname = getPkgName() + "." + className;
				Class<?> c = Class.forName(fullClassname);
				ATaskManager instance = (ATaskManager) c.newInstance();
				if (instance == null) {
					GLogger.error("Could not instantiate object: " + fullClassname);
					return null;
				}
				instance.initialize(joConfig);
				return instance;
			}
		} catch (Exception e) {
			e.printStackTrace();
			GLogger.error(e.getMessage());
		}
		return null;
	}
	
	public GIMTask instantiateGIMManager(JSONObject joConfig) {
		try {
			String className = joConfig.getString(CLASS);
			String fullClassname = getPkgName() + "." + className;
			Class<?> c = Class.forName(fullClassname);
			GIMTask instance = (GIMTask) c.newInstance();
			if (instance == null) {
				GLogger.error("Could not instantiate object: " + fullClassname);
				return null;
			}
			instance.initialize(joConfig);
			if (! ((Boolean)instance.get(ARetrieveTask.PAUSED))) {
				instance.start();
			}
			return instance;
		} catch (Exception e) {
			e.printStackTrace();
			GLogger.error(e.getMessage());
			return null;
		}
	}
	
	public ATaskManager getTaskManager(String name) {
		if (_mapMgrs.containsKey(name)) {
			return _mapMgrs.get(name);
		} else {
			return null;
		}
	}
	
	public HashMap <String, ATaskManager> getTaskManagers() {
		return _mapMgrs;
	}
	
	public ATaskManager removeTaskManager(String name) {
		ATaskManager atm = _mapMgrs.remove(name);
		if (atm != null) {
			atm.dispose();
		}
		return atm;
	}
	
	public GIMTask getGIMManager(String serviceName) {
		if (_gimMgrs.containsKey(serviceName)) {
			return _gimMgrs.get(serviceName);
		} else {
			return null;
		}
	}
	
	public HashMap <String, GIMTask> getGIMManagers() {
		return _gimMgrs;
	}
	
	public GIMTask removeGIMManager(String serviceName) {
		GIMTask gim = _gimMgrs.remove(serviceName);
		if (gim != null) {
			gim.disconnect();
		}
		return gim;
	}
	
	public JSONArray getJSON() {
		JSONArray ja = new JSONArray();
		if (_mapMgrs != null) {
			for (String key : _mapMgrs.keySet()) {
				ja.put(_mapMgrs.get(key).getJson());
			}
		}
		if (_gimMgrs != null) {
			for (String key : _gimMgrs.keySet()) {
				ja.put(_gimMgrs.get(key).getJson());
			}
		}
		return ja;
	}

	public JSONObject getJSONObject() {
		return _taskConfigs;
	}

	
	public void dispose() {
		for (String key : _mapMgrs.keySet()) {
			_mapMgrs.get(key).dispose();
		}
		for (String key : _gimMgrs.keySet()) {
			_gimMgrs.get(key).dispose();
		}

	}

	@Override
	public void run() {
//		try {
//			Thread.sleep(10000); // sleep for 10 seconds to allow debugger to attach
//			this.buildTaskMgrs();
//
//		} catch (InterruptedException e) {
//		}
	}

}
