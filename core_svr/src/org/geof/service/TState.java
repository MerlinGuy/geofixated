package org.geof.service;

import org.json.JSONException;
import org.json.JSONObject;

public class TState {
	int _step = 0;
	int _total_steps = -1;
	boolean _complete = false;
	Transaction _trans = null;
	String error = null;

	public TState (Transaction trans, int total_steps) {
		_trans = trans;
		_step = 0;
		_total_steps = total_steps;
	}
	
	public void log(String message) throws JSONException {
		log(message,false);
	}
	
	public void log(String message, boolean complete) throws JSONException {
		JSONObject jo = new JSONObject();			
		jo.put("step", ++_step);
		jo.put("total_steps", _total_steps);
		jo.put("message", message);
		if (error != null) {
			jo.put("error", error);
		}
		if (complete) {
			jo.put("complete", true);
		}
		TStateMgr.add(_trans, jo);
	}
	
	public void logError(String error)  {
		this.error = error;
		try {
			this.log("Error Occurred", true);
		} catch (JSONException e) {
			org.geof.log.GLogger.error("TState.logError: " + e.getMessage());
		}
	}
}
