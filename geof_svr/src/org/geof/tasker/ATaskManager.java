package org.geof.tasker;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.geof.log.GLogger;
import org.geof.util.DateUtil;
import org.json.JSONArray;
import org.json.JSONException;
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
public abstract class ATaskManager implements Runnable {

	public static final String NAME = "name";
	public static final String INTERVAL = "interval";
	public static final String INITIAL_DELAY = "initial_delay";
	public static final String MAX_PER_DAY = "max_per_day";
	public static final String TASK = "task";

	protected int _interval = 30000; // Check each 30 seconds.
	protected int _initial_delay = 10000; // Time to wait before the first run
	protected int _max_per_day = 100; // Maximum runs in a 24 hour period.
	protected int _run_count = 0;
	protected int _day_of_month;

	protected Thread _thread = null;
	protected boolean _alive = false;
	protected JSONObject _jsonConfig = null;
	protected String _name = null;
	protected boolean _paused = false;
	protected Date _lastRun = null;

	protected HashMap<String, ARetrieveTask> _mapTasks = new HashMap<String, ARetrieveTask>();

	public ATaskManager() {
	}

	public void initialize(JSONObject joConfig) {
		_day_of_month = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

		reinit(joConfig);
		
		_thread = new Thread(this);
		_thread.setName(_name);
		_thread.start();
	}
	
	public void reinit(JSONObject joConfig) {
		_jsonConfig = joConfig;
		_name = joConfig.optString( NAME, "");
		_interval = joConfig.optInt( INTERVAL, _interval);
		_initial_delay = joConfig.optInt( INITIAL_DELAY, _initial_delay);
		_max_per_day = joConfig.optInt( MAX_PER_DAY, _max_per_day);
		
		buildRetrieveTasks();
	}

	@Override
	public void run() {
		try {
			Thread.sleep(_initial_delay);
		} catch (InterruptedException e) {
		}
		
		GLogger.info("-- Started " + this.getClass().getName());
		try {
			_alive = true;
			while (_alive) {
				if (!_paused) {
					for (String key : _mapTasks.keySet()) {
						ARetrieveTask art = _mapTasks.get(key);
						if (!art.paused()) {
							JSONArray jarray = art.retrieve();
							processReturn(jarray, art);
						}
					}
					_lastRun = new Date();
					_run_count++;
					int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
					if (today != _day_of_month) {
						_run_count = 0;
						_day_of_month = today;
					}
					if (_max_per_day > 0) {
						_paused = _max_per_day <= _run_count;
					}
				}
				try {
					Thread.sleep(_interval);
				} catch (InterruptedException e) {
				}
			}
		} catch (Exception e) {
			GLogger.error(e);
		} finally {
			dispose();
		}
	}

	public boolean execute(String[] taskNames) {
		if (taskNames == null || taskNames.length == 0) {
			taskNames = (String[]) _mapTasks.keySet().toArray();
		}
		for (String key : taskNames) {
			ARetrieveTask art = _mapTasks.get(key);
			if (!art.paused()) {
				JSONArray jarray = art.retrieve();
				processReturn(jarray, art);
			}
		}
		_run_count++;
		return true;
	}

	public boolean paused() {
		return _paused;
	}

	public void setPaused(boolean paused) {
		_paused = paused;
	}

	public void setMaxRun(int max_run) {
		_max_per_day = max_run;
	}

	public int getMaxRun() {
		return _max_per_day;
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

	protected abstract void processReturn(JSONArray jarray, ARetrieveTask prt);

	/**
	 * 
	 */
	public void dispose() {
		this._alive = false;
		if (this._thread != null) {
			GLogger.debug("Thread ended for: " + _thread.getName());
			this._thread.interrupt();
		}
		for (String key : _mapTasks.keySet()) {
			_mapTasks.get(key).dispose();
		}
		_mapTasks.clear();
	}

	/**
	 * Returns the status of the Tasker as a string
	 * 
	 * @return String field with Connection information as a list of Name/Value
	 *         pairs
	 */
	public String toString() {
		return "ATaskManager";
	}

	/**
	 * 
	 * @return The current number of items used by the Tasker
	 */
	public int size() {
		return 0;
	}

	public boolean isAlive() {
		return _alive;
	}

	public void kill(boolean alive) {
		dispose();
	}

	public String getPkgName() {
		String name = this.getClass().getName();
		int lastIndx = name.lastIndexOf('.');
		String prefix = name.substring(0, lastIndx);
		int indx = name.indexOf("TaskManager");
		name = prefix + name.substring(lastIndx, indx).toLowerCase();
		return name;
	}

	private void buildRetrieveTasks() {
		try {
			for (String key : _mapTasks.keySet()) {
				_mapTasks.get(key).dispose();
			}
			_mapTasks.clear();
			JSONArray tasks = _jsonConfig.getJSONArray(TASK);
			int count = tasks.length();
			for (int indx = 0; indx < count; indx++) {
				JSONObject joTask = tasks.getJSONObject(indx);
				ARetrieveTask tm = instantiateTask(joTask);
				if (tm != null) {
					tm.setTaskMgr(this);
					_mapTasks.put(tm.name(), tm);
				}
			}
		} catch (JSONException e) {
			GLogger.error(e);
		}
	}

	protected JSONObject getJson() {
		JSONObject jo = new JSONObject();
		try {
			jo.put("type", "email");
			jo.put("name", this._name);
			jo.put("interval", (this._interval / 1000));
			jo.put("initial_delay", (this._initial_delay / 1000));
			jo.put("paused", this._paused);
			jo.put("run_count", this._run_count);
			jo.put("max_run", this._max_per_day);
			if (this._lastRun != null) {
				jo.put("last_run", DateUtil.TSDateFormat.format(this._lastRun));
				long nextRun = this._lastRun.getTime() + this._interval - (new Date()).getTime();
				jo.put("next_run", DateUtil.getTimespan(nextRun));
			}
			JSONArray jaTasks = new JSONArray();
			jo.put("tasks", jaTasks);
			for (String key : _mapTasks.keySet()) {
				jaTasks.put(_mapTasks.get(key).getJson());
			}
		} catch (JSONException e) {
			GLogger.error(e);
		}
		return jo;
	}

	public ARetrieveTask instantiateTask(JSONObject config) {
		try {
			if (!config.has(NAME)) {
				GLogger.error("Task config is missing task name field");
			} else {
				String className = config.getString(NAME);
				String fullClassname = getPkgName() + "." + className;
				ARetrieveTask instance = null;
				Class<?> c = Class.forName(fullClassname);
				instance = (ARetrieveTask) c.newInstance();
				if (instance == null) {
					GLogger.error("Could not instantiate object: " + fullClassname);
					return null;
				}
				instance.initialize(config);
				return instance;
			}
		} catch (Exception e) {
			e.printStackTrace();
			GLogger.error(e.getMessage());
		}
		return null;
	}

	public String name() {
		return this._name;
	}
	public boolean hasFile(JSONObject json) { 
		return false;
	}
}
