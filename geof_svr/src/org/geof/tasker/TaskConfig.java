package org.geof.tasker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.geof.log.GLogger;
import org.json.JSONObject;

public class TaskConfig {
	
	public static final String TASKS = "tasks";
	public static final String NAME = "name";
	public static final String INTERVAL = "interval";
	public static final String MAX_PER_DAY = "max_per_day";
	public static final String INITIAL_DELAY = "initial_delay";
	public static final String SOURCES = "sources";
	//TODO: move this to the 
	public static final String LOGIN = "login";
	public static final String PWD = "PWD";

	private JSONObject _taskConfigs = new JSONObject();
    private static TaskConfig _instance = null;
    
    /** The private constructor **/
    private TaskConfig(String filePath) {
    	this.parseConfigFile(filePath);
    }

    public static TaskConfig getInstance() throws IllegalArgumentException {
    	return getInstance(null);
    }
    
    public static TaskConfig getInstance(String filePath) throws IllegalArgumentException {
        if (_instance == null) {
            synchronized(TaskConfig.class) {
               if (_instance == null) {
            	   if (filePath == null) {
            		   throw new IllegalArgumentException();
            	   } else {
            		   _instance = new TaskConfig(filePath);
            	   }
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
    
    public JSONObject getJSONObject() {
    	return _taskConfigs;
    }

    /**
	 * Reads the tasker.conf file and build a JSONObject containing 
	 * each tasker to create and it's config.
	 * @param fileName  The full path to the tasker.conf file to read
	 * @return  Returns the JSONObject of all taskers.
	 */
	private boolean parseConfigFile(String filePath) {

		FileInputStream stream = null;
		boolean rtn = false;
		try {
			stream = new FileInputStream(new File(filePath));
			FileChannel fc = stream.getChannel();
			long size = fc.size();
			FileChannel.MapMode mode = FileChannel.MapMode.READ_ONLY;
			MappedByteBuffer bb = fc.map(mode, 0, size);
			/* Instead of using default, pass in a decoder. */
			String jsonString = Charset.defaultCharset().decode(bb).toString();
			this._taskConfigs = new JSONObject(jsonString);
			rtn = true;
		} catch (Exception e) {
			GLogger.error(e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					GLogger.error(e);
				}
			}
		}
		return rtn;
	}

}
