package org.geof.request;

import org.geof.tasker.ARetrieveTask;
import org.geof.tasker.ATaskManager;
import org.geof.tasker.TaskMgrConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TaskmgrRequest extends Request {

	private JSONObject _where = null;
	private ATaskManager _taskMgr = null;
	private ARetrieveTask _retrieveTask = null;
	private TaskMgrConfig _tmConfig = null;
	
	@Override
	/*
	 * Processes Read and Delete calls from clients.
	 * 
	 * @return Returns true if the call was a Read or Delete and successfull otherwise false.
	 */
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		} else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		} else if (_actionKey.equalsIgnoreCase(EXECUTE)) {
			return execute();
		}
		return false;
	}

	/**
	 * Reads all active database connections and writes them to the response stream as a list.
	 * 
	 * @return Returns true if no errors occurr.
	 */
	private boolean read() {
		try {
			JSONArray tasks = TaskMgrConfig.getInstance().getJSON() ;
			_jwriter.writePair("tasks", tasks);
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	private boolean update() {
		try {
			boolean hasPause = _data.has("pause");
			boolean hasMaxRun = _data.has("max_run");
			if ((! hasPause) && (! hasMaxRun)) {
				setError("Missing both pause and max_run");
				return false;
			}

			if (!checkRequirements()) {
				return false;
			}
			
			if (hasMaxRun) {
				_taskMgr.setMaxRun(_data.getInt("max_run"));
			}
			if (hasPause) {
				if (_retrieveTask != null) {
					_retrieveTask.setPaused(_data.getBoolean("pause"));
				} else {
					_taskMgr.setPaused(_data.getBoolean("pause"));
				}
			}
			return true;
		} catch (JSONException e) {
			setError(e.getMessage());
			return false;
		}
	}
	/**
	 * Removed either the entire ATaskManager and all of it's tasks or just the ARetrieveTask
	 * depending of if the WHERE object contains a "task_name" field
	 * 
	 * @return Returns true if no errors occurr and something was removed.
	 */
	private boolean delete() {
		if (!checkRequirements()) {
			return false;
		}
		
		if (_where.has("task_name")) {
			if (_retrieveTask == null) {
				_error = "Task does not exist: " + _retrieveTask.name();
				return false;
			} else {
				_taskMgr.removeTask(_retrieveTask.name());
			}
		} else {
			if (_taskMgr == null) {
				_error = "TaskManager does not exist: " + _taskMgr.name();
				return false;
			} else {
				_tmConfig.removeTaskManager(_taskMgr.name());
			}
		}
		return true;
	}
	
	private boolean execute() {
		if (!checkRequirements()) {
			return false;
		}
		
		String[] taskNames = null;
		if (_where.has("task_names")) {
			try {
				taskNames = _where.getString("task_names").split(",");
			} catch (JSONException e) {
				setError(e);
				return false;
			}
		}
		return _taskMgr.execute(taskNames);
			
	}
	private boolean checkRequirements() {
		try {
			if (!_data.has(WHERE)) {
				_error = "Missing where field";
				return false;
			}
			_where = _data.getJSONObject(WHERE);
			if (! _where.has("mgr_name")) {
				_error = "Missing TaskManager field";
				return false;			
			}
			_tmConfig = TaskMgrConfig.getInstance();
			_taskMgr = _tmConfig.getTaskManager(_where.getString("mgr_name"));
			
			if (_where.has("task_name")) {
				_retrieveTask = _taskMgr.getTask(_where.getString("task_name"));
			}
			return true;
		} catch (JSONException e) {
			_error = e.getMessage();
			return false;
		}
	}

}