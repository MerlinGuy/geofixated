package org.geof.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geof.prop.GlobalProp;
import org.json.JSONArray;

/**
 * Derived ABaseRequest class which retrieve log file contents
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class ConfigurationRequest extends Request {

	public final static String PROPERTIES = "properties";
	public final static String NAME = "name";
	public final static String VALUE = "value";
	public final static String IS_DB = "isdb";
	
	public final static String MAX_GUESTS = "max_guests";
	public final static String MAX_USR_GUESTS = "max_usr_guests";

	private final static  List<String> private_keys = Arrays.asList("basepath","pwd","password","admin","login","email","connstr","root","address");
	
	/**
	 * @see ABaseRequest.process()
	 * @return Returns true if the action is processes sucessfully otherwise false. 
	 * 
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		}
		else if (_actionKey.equalsIgnoreCase(READ + ".guest_info")) {
			return readGuestInfo();
		}
		else if (_actionKey.equalsIgnoreCase(READ + ".reload")) {
			return readReload();
		}
		return false;
	}

	public boolean readReload() {
		return GlobalProp.instance().reload(_dbi);
	}
	
	public boolean read() {
		GlobalProp gp = GlobalProp.instance();
		List<String> keys = null;
		JSONArray wKeys = _where.names();
				
		if (wKeys != null && wKeys.length() > 0) {
			int whereCount = wKeys.length();
			keys = new ArrayList<String>();
			String wkey;
			for (int indx=0; indx < whereCount; indx ++) {
				wkey = wKeys.optString(indx);
				keys.add(wkey);
			}
			
		} else {
			keys = gp.getSortedKeys();
		}
		
		_jwriter.openArray(DATA);
		GlobalProp.GProperty gprop = null;
		for (String key : keys) {
			boolean canAdd = true;
			for (String pkey : private_keys) {
				if (key.toLowerCase().indexOf(pkey) != -1) {
					canAdd = false;
					break;
				}
			}
			if (canAdd) {
				gprop = gp.getProp(key);
				if (gprop != null) {
					_jwriter.openObject();
					_jwriter.writePair(NAME, key);
					_jwriter.writePair(VALUE, gprop.value);
					_jwriter.writePair(IS_DB, gprop.isdb);
					_jwriter.closeObject();
				}
			}
		}
		_jwriter.closeArray();
		return true;
	}

	public boolean readGuestInfo() {
		GlobalProp gp = GlobalProp.instance();
		_jwriter.openArray(DATA);
		_jwriter.openObject();
		_jwriter.writePair(MAX_GUESTS, gp.get(MAX_GUESTS));
		_jwriter.writePair(MAX_USR_GUESTS, gp.get(MAX_USR_GUESTS));
		_jwriter.closeObject();
		_jwriter.closeArray();
		return true;
	}
}
