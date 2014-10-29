package org.geof.tasker;

import org.apache.commons.io.IOUtils;
import org.geof.log.GLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;

import javax.mail.Part;

public abstract class ARetrieveTask {

	public static final String NAME = "name";
	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String CLASS = "class";
	public static final String LOGIN = "login";
	public static final String PWD = "pwd";
	public static final String DOWNLOAD_DIR = "downloadDir";
	public static final String SUBJECTS = "subjects";
	public static final String DRY_RUN = "dry_run";
	public static final String DELETE_AFTER_DOWNLOAD = "delete_after_download";
	public static final String INTERVAL = "interval";
	public static final String MAX_PER_DAY = "max_per_day";
	public static final String PAUSED = "paused";
	

	public String[] _keys = {NAME,HOST,PORT,CLASS,LOGIN,PWD,DOWNLOAD_DIR,SUBJECTS,
			DRY_RUN,DELETE_AFTER_DOWNLOAD,INTERVAL,MAX_PER_DAY,PAUSED};

	
	protected HashMap<String, Object> _data = null;
	protected String _error_msg = null;
	protected int _run_count = 0;
	
	protected ATaskManager _taskMgr = null;
	protected boolean _isAuthorized = false;

	public ARetrieveTask() {}

	public void initialize(JSONObject joParams) {
		String key;
		for ( int indx=0;indx < _keys.length; indx++) {
			key = _keys[indx];
			if (joParams.has(key)) {
				try {
					_data.put(key, joParams.get(key));
				} catch (JSONException e) {
					GLogger.error(e);
				}
			}
		}
	}
	
	public void setTaskMgr(ATaskManager taskMgr) {
		_taskMgr = taskMgr;
	}

	public JSONObject getJson() {
		JSONObject jo = new JSONObject();
		try {
			Iterator<String> iKeys = _data.keySet().iterator();
			String key;
			while (iKeys.hasNext()) {
				key = iKeys.next();
				jo.put(key, _data.get(key).toString());
			}
		} catch (JSONException e) {
			GLogger.error(e);
		}
		return jo;
	}

	public abstract void dispose();

	public String name() {
		return (String)_data.get(NAME);
	}

	public Object get(String key) {
		return _data.get(key);
	}
	
	public Boolean paused() {
		return (Boolean)_data.get(PAUSED);
	}

	public void setPaused(boolean paused) {
		_data.put(PAUSED,paused);
	}

	public String getErrorMsg() {
		return _error_msg;
	}

	public boolean isAuthorized() {
		return _isAuthorized;
	}

	public boolean hasError() {
		return (_error_msg == null);
	}

	public void clearError() {
		_error_msg = null;
	}

	public JSONArray retrieve() {
		_run_count++;
		return null;
	}

	public int runCount() {
		return _run_count;
	}

	public static String decodeName(String name) throws Exception {
		if (name == null || name.length() == 0) {
			return "unknown";
		}
		String ret = java.net.URLDecoder.decode(name, "UTF-8");
		// also check for a few other things in the string:
		ret = ret.replaceAll("=\\?utf-8\\?q\\?", "");
		ret = ret.replaceAll("\\?=", "");
		ret = ret.replaceAll("=20", " ");
		return ret;
	}

	public static JSONObject saveFile(File saveFile, Part part) throws Exception {

		try {
			FileOutputStream fos = new FileOutputStream(saveFile);
			BufferedOutputStream bos = new BufferedOutputStream(fos);

			byte[] buff = new byte[2048];
			InputStream is = part.getInputStream();
			MessageDigest md = MessageDigest.getInstance("MD5");
			is = new DigestInputStream(is, md);
			int ret = 0, size = 0;
			while ((ret = is.read(buff)) > 0) {
				bos.write(buff, 0, ret);
				size += ret;
			}
			bos.close();
			is.close();

			String checksumval = "";
			byte[] bytes = md.digest();
			for (int i = 0; i < bytes.length; i++) {
				checksumval += Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1);
			}
			JSONObject json = new JSONObject();
			json.put("filesize", size);
			json.put("checksumval", checksumval);
			return json;
		} catch (Exception e) {
			GLogger.error(e);
			return null;
		}
	}

	public static String readBodyText(Part part) throws Exception {

		try {
			InputStream is = part.getInputStream();
			StringWriter writer = new StringWriter();
			IOUtils.copy(is, (Writer) writer);
			String text = writer.toString();
			int start = text.indexOf("{");
			int end = text.indexOf("}");
			if ((start == -1) || (end == -1) || (end < start)) {
				return "";
			}
			return text.substring(start - 1, end + 1).trim();

		} catch (Exception e) {
			GLogger.error(e);
			return null;
		}
	}

}