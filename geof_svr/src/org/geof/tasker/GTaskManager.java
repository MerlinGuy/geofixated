package org.geof.tasker;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONObject;

/**
 * Abstract base class for Runnable Task Objects which manage external resources
 * on their own threads. This includes timed tasks.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class GTaskManager {

	public static final String INITIAL_DELAY = "initial_delay";

	protected int _day_of_month;

	protected boolean _alive = false;
	protected JSONObject _jsonConfig = null;
	protected String _name = null;
	protected boolean _paused = false;
	protected Date _lastRun = null;

	protected HashMap<String, ARetrieveTask> _mapTasks = new HashMap<String, ARetrieveTask>();

	public GTaskManager() {
	}

	public void initialize(JSONObject joConfig) {
		_day_of_month = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

//		reinit(joConfig);
	}
	
//	public void reinit(JSONObject joConfig) {
//		_jsonConfig = joConfig;
//		_name = JsonUtil.optString(joConfig, NAME, "");
//		_interval = JsonUtil.optInt(joConfig, INTERVAL, _interval);
//		_initial_delay = JsonUtil.optInt(joConfig, INITIAL_DELAY, _initial_delay);
//		_max_per_day = JsonUtil.optInt(joConfig, MAX_PER_DAY, _max_per_day);
//		
//		buildRetrieveTasks();
//	}

	public boolean execute(String[] taskNames) {
		if (taskNames == null || taskNames.length == 0) {
			taskNames = (String[]) _mapTasks.keySet().toArray();
		}
		for (String key : taskNames) {
			ARetrieveTask art = _mapTasks.get(key);
			if (!art.paused()) {


			
			
			
			
			}
		}
		return true;
	}

	public ARetrieveTask getTask(String retrieveTaskName) {
		if (_mapTasks.containsKey(retrieveTaskName)) {
			return _mapTasks.get(retrieveTaskName);
		} else {
			return null;
		}
	}

	public ARetrieveTask removeTask(String retrieveTaskName) {
		ARetrieveTask art = _mapTasks.remove(retrieveTaskName);
		if (art != null) {
			art.dispose();
		}
		return art;
	}

	public boolean addTask(ARetrieveTask task) {
		if (_mapTasks.containsKey(task.name())) {
			return false;
		}
		_mapTasks.put(task.name(), task);
		return true;
	}

	/**
	 * 
	 */
	public void dispose() {
		for (String key : _mapTasks.keySet()) {
			_mapTasks.get(key).dispose();
		}
		_mapTasks.clear();
	}

	public String getPkgName() {
		String name = this.getClass().getName();
		int lastIndx = name.lastIndexOf('.');
		String prefix = name.substring(0, lastIndx);
		int indx = name.indexOf("TaskManager");
		name = prefix + name.substring(lastIndx, indx).toLowerCase();
		return name;
	}

//	private void buildRetrieveTasks() {
//		try {
//			for (String key : _mapTasks.keySet()) {
//				_mapTasks.get(key).dispose();
//			}
//			_mapTasks.clear();
//			JSONArray tasks = _jsonConfig.getJSONArray(TASK);
//			int count = tasks.length();
//			for (int indx = 0; indx < count; indx++) {
//				JSONObject joTask = tasks.getJSONObject(indx);
//				ARetrieveTask tm = instantiateTask(joTask);
//				if (tm != null) {
//					tm.setTaskMgr(this);
//					_mapTasks.put(tm.name(), tm);
//				}
//			}
//		} catch (JSONException e) {
//			GLogger.error(e);
//		}
//	}

//	protected JSONObject getJson() {
//		JSONObject jo = new JSONObject();
//		try {
//			jo.put("type", "email");
//			jo.put("name", this._name);
//			jo.put("interval", (this._interval / 1000));
//			jo.put("initial_delay", (this._initial_delay / 1000));
//			jo.put("paused", this._paused);
//			jo.put("run_count", this._run_count);
//			jo.put("max_run", this._max_per_day);
//			if (this._lastRun != null) {
//				jo.put("last_run", DateUtil.TSDateFormat.format(this._lastRun));
//				long nextRun = this._lastRun.getTime() + this._interval - (new Date()).getTime();
//				jo.put("next_run", DateUtil.getTimespan(nextRun));
//			}
//			JSONArray jaTasks = new JSONArray();
//			jo.put("tasks", jaTasks);
//			for (String key : _mapTasks.keySet()) {
//				jaTasks.put(_mapTasks.get(key).getJson());
//			}
//		} catch (JSONException e) {
//			GLogger.error(e);
//		}
//		return jo;
//	}
//
//	public ARetrieveTask instantiateTask(JSONObject config) {
//		try {
//			if (!config.has(NAME)) {
//				GLogger.error("Task config is missing task name field");
//			} else {
//				String className = config.getString(NAME);
//				String fullClassname = getPkgName() + "." + className;
//				ARetrieveTask instance = null;
//				Class<?> c = Class.forName(fullClassname);
//				instance = (ARetrieveTask) c.newInstance();
//				if (instance == null) {
//					GLogger.error("Could not instantiate object: " + fullClassname);
//					return null;
//				}
//				instance.initialize(config);
//				return instance;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			GLogger.error(e.getMessage());
//		}
//		return null;
//	}

	public String name() {
		return this._name;
	}
	public boolean hasFile(JSONObject json) { 
		return false;
	}
}
