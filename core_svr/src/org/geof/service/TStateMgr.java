package org.geof.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geof.log.GLogger;
import org.json.JSONException;
import org.json.JSONObject;

public class TStateMgr {

	private static HashMap<String, ArrayList<JSONObject>> _map = null;

	public static void initialize() {
		if (_map == null) {
			_map = new HashMap<String, ArrayList<JSONObject>>();
			GLogger.writeInit("*** TransStatMgr.initialized");
		}
	}
	
	public static void add(Transaction trans, String msg ) {
		JSONObject jo = new JSONObject();
		try {
			jo.put("message", msg);
			add(trans,jo);
		} catch (JSONException e) {
			GLogger.error(e);
		}
		
	}
	public static void add(Transaction trans, JSONObject joMsg ) {
		String key = trans.getSessionID() + ":" + trans.getID();
		ArrayList<JSONObject> list = _map.get(key);
		if (list == null) {
			list = new ArrayList<JSONObject>();
			_map.put(key, list);
		}
		list.add(joMsg);
	}
	
	public static List<JSONObject> get(Transaction trans, boolean remove) {
		return get(trans.getSessionID() + ":" + trans.getID(), remove);
	}

	public static List<JSONObject> get(String key, boolean remove) {
		List<JSONObject> list = _map.get(key);
		
		if (list == null) {
			return new ArrayList<JSONObject>();
		} else {
			return remove ? _map.remove(key) : _map.get(key);
		}
	}

	public static JSONObject next(Transaction trans, boolean remove) {
		String key = trans.getSessionID() + ":" + trans.getID();
		List<JSONObject> list = _map.get(key);
		
		if (list != null && list.size() > 0) {
			return remove ? list.remove(0) : list.get(0);
		} else {
			return new JSONObject();
		}
	}
	
	public static void clear(String key) {
		_map.remove(key);
	}
	
	public static void clear(Transaction trans) {
		String key = trans.getSessionID() + ":" + trans.getID();
		_map.remove(key);
	}
	
	public static void clearSession(GSession session) {
		String sid = session.getSessionID() + ":";
		List<String> list = new ArrayList<String>();
		for (String key : _map.keySet()) {
			if (key.startsWith(sid)) {
				list.add(key);
			}
		}
		for (String key : list) {
			_map.remove(key);
		}
	}

}


