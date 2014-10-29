package org.geof.prop;

import java.io.File;
import java.io.FileInputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geof.db.DBInteract;
import org.geof.log.GLogger;
import org.geof.service.FileChangeWatcher;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parent class and static properties holder and source for all Globally accessed application
 * properties.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class GlobalProp {
	public static final String OPEN_REQUESTS = "open_requests";
	public static final String OVERLOAD = "overload";
	public static final String DB = "database";
	public static final String FILE = "file";

	private HashMap<String, GProperty> _properties = new HashMap<String, GProperty>();
	private boolean _hasError = false;
	private String _errorMsg = null;
	private File _file = null;
	private FileChangeWatcher _fileChangeWatcher = null;
	private static ExecutorService _executor = null;
	
	private static GlobalProp _instance = null;
	private static final List<?> NO_PRINT = Arrays.asList("pwd","password");
	private final static String sql = "SELECT name,value FROM serverconfig";
	
	private static Set<String> _openRequests = null;
	private static Map<String,String> _overloads = null;	
	
	private final int EXECUTOR_THREAD_COUNT = 4;

	private GlobalProp(){
		initExecutor();
	}
	
	private void initExecutor() {
		try {
			_executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT, Executors.privilegedThreadFactory());
		} catch (Exception e) {
			GLogger.error(e);
		}
	}

	public static GlobalProp instance() {
		if (_instance == null) {
			_instance = new GlobalProp();
		}
		return _instance;
	}
	
	public boolean reload(DBInteract dbi) {
		boolean rtn = false;
		FileInputStream fis = null;
		
		try {
			if (_file.exists()) {
				fis = new FileInputStream(_file);
				Properties pf = new Properties();
				pf.load(fis);
				Enumeration<?> names = pf.propertyNames();
				while (names.hasMoreElements()) {
					String name = (String)names.nextElement();
					put(name.toLowerCase(), pf.getProperty(name), false);
				}
				rtn = true;
			}
			
			String[] vals = this.get(OPEN_REQUESTS,"").split(",");
			_openRequests = new HashSet<String>( Arrays.asList(vals));
			
			_overloads = new HashMap<String,String>( );
			vals = this.get(OVERLOAD,"").split(";");
			for (String val : vals) {
				val = val.trim();
				if (val.length() > 0) {
					String[] parts = val.split(">");
					if (parts.length == 2) {
						_overloads.put(parts[0], parts[1]);
					}
				}
			}
			
			if (dbi != null) {
				this.populate(dbi);
			}

		} catch (Exception e) {
		} finally {
			if (fis != null) {
				try { fis.close(); } catch (Exception e) { }
			}
		}
		return rtn;

	}
	/**
	 * This Method reads in all the server properties stored in the server config file.  
	 * Usually this file is named init.prop
	 * 
	 * @param filepath  Full path to property file
	 * @return  True if the initialization method succeeded otherwise false if an error occurrs.
	 */

	public boolean populate(String filepath) {
		return populate(new File(filepath));
	}
	
	public boolean populate(File file) {
		_file = file;
		boolean rtn = false;
	
		if (file.exists()) {
			rtn = this.reload(null);
			runWatchService();
			return rtn;
		}
		return false;
	}
	
	/**
	 * This Method reads in all the properties stored in the serverconfig table of the
	 * application's database
	 * 
	 * @param dbConn  DBConnection object to use for database access.
	 * @return  True if the initialization method succeeded otherwise false if an error occurrs.
	 */
	public boolean populate(DBInteract dbi) {
		boolean rtn = false;
		if (dbi == null) {
			GLogger.writeInit("GlobalProp.initialize : dbi = null");
			return rtn;
		}
		ResultSet rs = null;
		try {
			String key;
			rs = dbi.getResultSet(sql, null);
			if (rs != null) {
				while (rs.next()) {
					key = rs.getString(1);
					if (! this.hasKey(key)) {
						put(key, rs.getString(2), true);
					}
				}
				rs.close();
			} else {
				GLogger.writeInit("GlobalProp.initialize: Resultset is null");
			}
			rtn = true;
		} catch (Exception e) {
			GLogger.error(e);
		}
		return rtn;
	}
	
	private void runWatchService() {
		_fileChangeWatcher = new FileChangeWatcher(this);
		_fileChangeWatcher.initialize(_file.getPath());
		_executor.execute(_fileChangeWatcher);
    }
	
	/**
	 * Adds a new property to the _properties list
	 * 
	 * @param key Property key name
	 * @param value Property value
	 * @return Returns the old value if the property was prevously set otherwise null
	 */
	public String put(String key, String value, boolean isDB) {
		String oldValue = (hasKey(key) ? get(key) : null);
		GProperty gp = new GProperty(key,value,isDB);
		_instance._properties.put(key, gp);
		return oldValue;
	}

	/**
	 * Returns the property value associated to the key
	 * 
	 * @param key Name of the property to retieve
	 * @return Returns the property value if the key exists in the property list otherwise it
	 * returns null
	 */
	public String get(String key) {
		GProperty gp = _instance._properties.get(key);
		return (gp != null) ? gp.value : null; 
	}

	public static String getProperty(String key){
		return _instance == null ? null : _instance.get(key);
	}
	
	public static boolean isOpenRequest(String key) {
		return _openRequests == null ? null :_openRequests.contains(key);
	}
	
	public static String getOverload(String key, String dflt) {
		if (_overloads != null && _overloads.containsKey(key)) {
			return _overloads.get(key);
		} else {
			return dflt;
		}
	}
	/**
	 * Returns the String property value associated to the key
	 * 
	 * @param key Name of the property to retrieve
	 * @param dftValue The default value to return if the property key does not exist in the
	 * list
	 * @return Returns the property value if the key exists in the property list otherwise it
	 * returns the passed in default string value
	 */
	public String get(String key, String dftValue) {
		if (_instance == null) {
			return null;
		}
		GProperty gp = _instance._properties.get(key);
		return gp == null ? null : gp.value;
	}

	/**
	 * Returns the int property value associated to the key
	 * 
	 * @param key Name of the property to retieve
	 * @param dftValue The default value to return if the property key does not exist in the
	 * list
	 * @return Returns the property value if the key exists in the property list otherwise it
	 * returns the passed in default it value
	 */
	public int getInt(String key, int dftValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {}
		}
		return dftValue;
	}

	/**
	 * Returns the boolean property value associated to the key
	 * 
	 * @param key Name of the property to retieve
	 * @param dftValue The default value to return if the property key does not exist in the
	 * list
	 * @return Returns the property value if the key exists in the property list otherwise it
	 * returns the passed in default boolean value
	 */
	public boolean getBool(String key, boolean dftValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Boolean.parseBoolean(value);
			} catch (Exception e) {}
		}
		return dftValue;
	}

	/**
	 * Returns the long property value associated to the key
	 * 
	 * @param key Name of the property to retieve
	 * @param dftValue The default value to return if the property key does not exist in the
	 * list
	 * @return Returns the property value if the key exists in the property list otherwise it
	 * returns the passed in default long value
	 */
	public long getLong(String key, long dftValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Long.parseLong(value);
			} catch (Exception e) {}
		}
		return dftValue;
	}

	public GProperty getProp(String key) {
		return _instance._properties.get(key);
	}

	/**
	 * Removes the property from the properties list
	 * 
	 * @param key Key of the property to remove
	 * @return Returns the stored value if it existed in the properties list otherise it
	 * returns null
	 */
	public String remove(String key) {
		String value = get(key);
		_properties.remove(key);
		return value;
	}

	/**
	 * Check to see if the passed in key exists in properties map
	 * @param key  Name of property to check  
	 * @return  Returns true if the property key exists otherwise false
	 */
	public boolean hasKey(String key) {
		return _properties.containsKey(key);
	}

	/**
	 * Prints all properties to the log file if the default log level is Debug or higher.
	 */
	public void printToInitLog() {
		Iterator<String> iter = getSortedKeys().iterator();
		GLogger.writeInit(this.getClass().getSimpleName());
		while (iter.hasNext()) {
			String key = iter.next();
			if (! NO_PRINT.contains(key.toLowerCase())) {
				GLogger.writeInit(" " + key + ": " + _properties.get(key).value);
			}
		}
	}

	/**
	 *
	 * @return  Returns an ArrayList of all the properties keys
	 */
	public ArrayList<String> getKeys() {
		ArrayList<String> keys = new ArrayList<String>();
		Iterator<String> iter = _properties.keySet().iterator();
		while (iter.hasNext()) {
			keys.add(iter.next());
		}
		return keys;
	}

	public static ExecutorService getExecutor() {
		return _executor;
	}
	
	/**
	 * clear all property lists prior to Object dispose 
	 */
	public void dispose() {
		_properties.clear();
		_properties = null;
		shutdownWatchService();
		if (_executor != null) {
			_executor.shutdown();
		    try {
				_executor.awaitTermination(0, null);
			} catch (InterruptedException e) {
				GLogger.error(e);
			}
		    System.out.println("GlobalProp executor threads finished");
		    _executor = null;
		}
	}
	
	private void shutdownWatchService() {
		if (_fileChangeWatcher != null) {
			_fileChangeWatcher.dispose();
			_fileChangeWatcher = null;
		}
	}

	/**
	 * 
	 * @return  Returns true if GlobalProperties currently has an error message
	 */
	public boolean hasError() {
		return _hasError;
	}

	/**
	 * 
	 * @return  Returns the current error and resets the error value to null
	 */
	public String getError() {
		String errorMsg = _errorMsg;
		_hasError = false;
		_errorMsg = null;
		return errorMsg;
	}

	/**
	 * Sets the current error to the passed in error message
	 * @param errorMsg  Text to set the current to.
	 */
	protected void setError(String errorMsg) {
		getError();
		_errorMsg = errorMsg;
		_hasError = (_errorMsg != null);
	}

	/**
	 * 
	 * @return  Returns the list of properties in sort format.
	 */
	public ArrayList<String> getSortedKeys() {
		ArrayList<String> keys = new ArrayList<String>();
		Iterator<String> iter = _properties.keySet().iterator();
		while (iter.hasNext()) {
			keys.add(iter.next());
		}
		Collections.sort(keys, new KeyComparator());
		return keys;
	}
	
	public JSONObject asJson() {
		JSONObject jobj = new JSONObject();
		ArrayList<String> keys = this.getSortedKeys();
		for (String key : keys) {
			try {
				jobj.put(key, _instance.get(key));
			} catch (JSONException e) {
				GLogger.error(e);
			}
		}
		return jobj;
	}

	public class GProperty {
		public String name = null;
		public String value = null;
		public boolean isdb = false;
		
		public GProperty(String Name, String Value, boolean isDB) {
			name = Name;
			value = Value;
			isdb = isDB;
		}
	}
}
