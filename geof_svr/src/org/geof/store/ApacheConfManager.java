package org.geof.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.util.ExecTool;

/**
 * The ApacheConfManager adds new virtual directories to the apache2.conf file. This is used
 * to add new video streaming ability to file storage location on the server.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class ApacheConfManager {

	private static String CONFPATH = "/etc/apache2/";
	private static String CONFFILE = "apache2.conf";
	private static String APPENDLOC = null;

	private boolean _debugExec = true;
	private static String _errMsg = null;

	/**
	 * Class constructor
	 */
	public ApacheConfManager() {
	}

	/**
	 * Sets the script location used for appending the 
	 * @param cmdpath
	 */
	public void setAppendConfPath(String cmdpath) {
		APPENDLOC = cmdpath;
	}

	/**
	 * 
	 * @return true if ApacheConfManager has a current error otherwise false
	 */
	public boolean hasError() {
		return _errMsg != null;
	}

	/**
	 * 
	 * @return Returns the current error message and then reset the message to null.
	 */
	public String getErrorMessage() {
		String errorMessage = _errMsg;
		_errMsg = null;
		return errorMessage;
	}

	/**
	 * This method appends a new Alias name to the apache2.conf file
	 * @param aliasName  New alias name to append
	 * @param path Location on the server disk with Apache2 will use as the Directory Alias file source
	 * @return  True if no error occurs otherwise false
	 */
	public boolean AppendAlias(String aliasName, String path) {
		try {
			String appendFile = "";
			if (APPENDLOC == null) {
				String vsLocation = GlobalProp.getProperty("videoscriptloc");
				if (vsLocation == null) { // try to guess known location
					vsLocation = "/var/lib/tomcat6/scripts";
				} 
				appendFile = vsLocation + "/appendConf";
				if (!(new File(appendFile)).exists()){
					GLogger.error("appendConf file not found: " + appendFile);
					return false;
				}
				APPENDLOC = vsLocation;
			}
			// 1) use Runtime call to edit and reload apache2's config.
			String cmd = "sudo " + APPENDLOC + "/appendConf " + aliasName + " " + path + " " + APPENDLOC;
			ExecTool et = new ExecTool(_debugExec);
			boolean rtn = et.execute(cmd, true);// .exec(cmd);

			if (! rtn) {
				GLogger.error("appendConf did not execute correctly");
			}
			// 2) test new site
			// TODO: write code to test new Alias site
			return rtn;
		} catch (Exception e) {
			GLogger.error(e);
			return false;
		}
	}

	/**
	 * Checks to see if the alias name and file directory already existw in the apache2.conf file
	 * @param aliasName Alias name to check
	 * @param path File directory path to check
	 * @return True if the Alias already exists in the file otherwise false.
	 */
	public boolean hasAlias(String aliasName, String path) {
		if ((aliasName == null) || (aliasName.length() == 0)) {
			return false;
		}
		if ((path == null) || (path.length() == 0)) {
			return false;
		}

		String aliasPath = getAliasDirectory(aliasName);
		if (aliasPath == null) {
			return false;
		}
		return (path.compareTo(aliasPath) == 0);
	}

	/**
	 * Checks to see if the alias name already exist in the apache2.conf file
	 * @param aliasName Alias name to check
	 * @return True if the Alias already exists in the file otherwise false.
	 */
	public boolean hasAlias(String aliasName) {
		BufferedReader reader = null;
		boolean rtn = false;
		try {
			File confFile = new File(CONFPATH + CONFFILE);
			if (!confFile.exists()) {
				GLogger.error("Apache conf file not found: " + CONFPATH + CONFFILE);
				return false;
			} else {
				String alias = "Alias /" + aliasName + "/";
				reader = new BufferedReader(new FileReader(confFile));
				String line;

				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.startsWith(alias)) {
						rtn = true;
					}
				}
			}

		} catch (Exception e) {
			GLogger.error(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception eio) {}
			}
		}
		return rtn;
	}

	/**
	 * Searchs the apache2.conf file for an Alias name and returns the file directory if found.
	 * @param aliasName  Alias name to search for
	 * @return Returns the file directory if the alias name if found otherwise null. 
	 */
	public String getAliasDirectory(String aliasName) {
		BufferedReader reader = null;
		String rtn = null;
		try {
			File confFile = new File(CONFPATH + CONFFILE);
			if (confFile.exists()) {
				String alias = "Alias /" + aliasName + "/";
				reader = new BufferedReader(new FileReader(confFile));
				String line;

				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.startsWith(alias)) {
						int len = line.length();
						int indx = line.indexOf(alias) + alias.length();
						if (len < indx + 1) {
							break;
						}
						indx = line.indexOf("\"", indx);
						if (indx == -1) {
							break;
						}
						int indx2 = line.indexOf("\"", indx + 1);
						if (indx2 == -1) {
							break;
						}
						rtn = line.substring(indx + 1, indx2 - 1);
					}
				}
			}

		} catch (Exception e) {
			GLogger.error(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception eio) {}
			}
		}
		return rtn;
	}

}
