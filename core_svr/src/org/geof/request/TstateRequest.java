package org.geof.request;

import java.util.List;

import org.geof.service.TStateMgr;
import org.json.JSONObject;

public class TstateRequest extends Request {

	public static final String TRANS_ID = "transaction_id";
	
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(CREATE + ".test")) {
			return createTest();
		}
		return false;
	}

	public boolean read() {
		_jwriter.openArray(DATA);
		if (_session == null) {
			return setError("No Session");
		}
		String key = _session.getSessionID() + ":" + _where.optInt(TRANS_ID, -1);
		List<JSONObject> list = TStateMgr.get(key, true);
		for (JSONObject jo : list) {
			_jwriter.writeCommaPush();
			_jwriter.writeValue(jo);
		}
		_jwriter.closeArray();
		if (_data.optBoolean("complete")) {
			TStateMgr.clear(key);
		}
		return true;
	}
	
	public boolean createTest() {
		for ( int indx=0; indx<20; indx++) {
			TStateMgr.add(_transaction, "Tstate.createTest " + _session.getSessionID() + " : " + _transaction.getID() + " #" + indx);
		}
		return true;
	}
}
