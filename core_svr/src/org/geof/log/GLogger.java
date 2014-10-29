package org.geof.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.geof.util.ConvertUtil;
import org.geof.util.DateUtil;
import org.json.JSONArray;

/**
 * This class is the main logging tool for the application.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class GLogger {

	public final static int _ERROR = 0, _WARN = 1, _INFO = 2, _DEBUG = 3, _VERBOSE = 4;
	public final static int DEFAULT = _INFO;
	private String _errorMsg = null;
	private boolean _hasError = false;
	private static File _geof_log = null;
	private static File _init_log = null;
	
	private static boolean _append = false;
	private static int _loglevel = DEFAULT;
	private static String _filepath = "";

	private static GLogger _logger = null;
	private static BufferedWriter _out = null;
	
	private static boolean _write_time = false;
	
	public final static String[] _forbidden = {"login","pwd","password","passwd"}; 

	/**
	 * Class constructor
	 */
	private GLogger() {
	}


	// ---------------------------------------------
	/**
	 * Method initializes the Logger system
	 * 
	 * @param filepath
	 *            Path to location on disk to write the log file
	 * @param header
	 *            Header string to prepend to the log file when first opened.
	 * @param loglevel
	 *            Default Log level for limiting log writes
	 */
	public static void initialize(String filepath, String initlog, String header, int loglevel) {
		if (_logger == null) {
			_logger = new GLogger();
		}
		_filepath = filepath;
		_geof_log = new File(_filepath);
		_geof_log.delete();
		
		_loglevel = Math.max(_ERROR, Math.min(_VERBOSE, loglevel));

		_init_log = new File(initlog);
		_init_log.delete();
		
		writeInit("---------------------------------------------------");
		_append = true;
		
		if (header == null) {
			header = "Logging started at ";
		}
		String dateNow = DateUtil.dateTimeNow();
		writeInit(header + dateNow + "\n");
		writeLog(header + dateNow, true);
	}

	/**
	 * @param tail
	 *            Number of lines at the end of the file to return, use -1 to
	 *            return the whole file
	 * @return Returns either the full contents or the last n-number of lines of
	 *         the log file
	 */
	public static JSONArray getContents(int tail, boolean isInit) {
		JSONArray contents = new JSONArray();
		try {
			File file = (isInit) ? _init_log : _geof_log;
			int forbid_len = _forbidden.length;
			
			if (file.exists()) {
				ArrayList<String> aLines = (tail > 0) ? LogReader.tail(file, tail) : LogReader.readFile(file);
				for (String line : aLines) {
					boolean found = false;
					for (int indx=0; (!found) && (indx < forbid_len); indx++) {
						if (line.indexOf(_forbidden[indx]) != -1) {
							found = true;
						}
					}
					if (!found) {
						contents.put(ConvertUtil.encode(line));
					}
				}
			} else {
				String line = "Log file does not exist: " + _filepath;
				contents.put(ConvertUtil.encode(line));
			}
		} catch (Exception e) {
			GLogger.error("Logger.getContents " + e.getMessage());
		}
		return contents;
	}

	public static String getFilePath() {
		return _filepath;
	}
	/**
	 * 
	 * @return Returns the Singleton instance object
	 */
	public static GLogger getInstance() {
		return _logger;
	}

	/**
	 * Sets the default log level
	 * 
	 * @param loglevel
	 *            Enumeration value of the log level
	 */
	public static void setLogLevel(int loglevel) {
		_loglevel = Math.max(_ERROR, Math.min(_VERBOSE, loglevel));
	}

	/**
	 * 
	 * @return Returns the current level of logging
	 */
	public static int getLogLevel() {
		return _loglevel;
	}

	public static File getLogFile() {
		return _geof_log;
	}
	
	/**
	 * Writes Text to log file if log level is set to VERBOSE or above
	 * 
	 * @param text
	 *            Text to write to the log file.
	 */
	public static void verbose(String text) {
		write(_VERBOSE, text);
	}
	public static void verbose(String text, Object... values) {
		text = String.format(text, values);
		write(_VERBOSE, text);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to VERBOSE or above
	 * 
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void verbose(Exception e) {
		write(_VERBOSE, e);
	}

	/**
	 * Writes Text to log file if log level is set to DEBUG or above
	 * 
	 * @param text
	 *            Text to write to the log file.
	 */
	public static void debug(String text) {
		write(_DEBUG, text);
	}
	
	public static void debugNoNl(String text) {
		writeNoNl(_DEBUG, text);
	}
	
	public static void debug(String text, Object... values) {
		text = String.format(text, values);
		write(_DEBUG, text);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to DEBUG or above
	 * 
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void debug(StackTraceElement[] ste) {
		write(_DEBUG, ste);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to DEBUG or above
	 * 
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void debug(Exception e) {
		write(_DEBUG, e);
	}

	/**
	 * Writes Text to log file if log level is set to INFO or above
	 * 
	 * @param text
	 *            Text to write to the log file.
	 */
	public static void info(String text) {
		write(_INFO, text);
	}
	public static void info(String text, Object... values) {
		text = String.format(text, values);
		write(_INFO, text);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to INFO or above
	 * 
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void info(Exception e) {
		write(_INFO, e);
	}

	/**
	 * Writes Text to log file if log level is set to WARN or above
	 * 
	 * @param text
	 *            Text to write to the log file.
	 */
	public static void warn(String text) {
		write(_WARN, text);
	}
	public static void warn(String text, Object... values) {
		text = String.format(text, values);
		write(_WARN, text);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to WARN or above
	 * 
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void warn(Exception e) {
		write(_WARN, e);
	}

	/**
	 * Writes Text to log file if log level is set to ERROR or above
	 * 
	 * @param text
	 *            Text to write to the log file.
	 */
	public static void error(String text) {
		write(_ERROR, text);
	}

	public static void error(String text, Object... values) {
		text = String.format(text, values);
		write(_ERROR, text);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to ERROR or above
	 * 
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void error(Exception e) {
		write(_ERROR, e);
	}

	/**
	 * Writes Exceptiong to log file if log level is set to VERBOSE or above
	 * 
	 * @param header
	 *            Header string to write to file before writing the exception
	 * @param e
	 *            Exception to write to the log file.
	 */
	public static void error(String header, Exception e) {
		write(_ERROR, header);
		write(_ERROR, e);
	}

	/**
	 * Writes the text to the log file regardless of logging level
	 * 
	 * @param text
	 *            Text to write to the log file.
	 */
	public static void forceWrite(String text) {
		writeLog(text,true);
	}

	public static void forceWrite(String text, Object... values) {
		text = String.format(text, values);
		writeLog(text,true);
	}

	/**
	 * Writes Text to log file if log level is set to DEBUG or above
	 * 
	 * @param text
	 *            Text to write to the log file.
	 * @param prepentTime
	 *            If true the logger prepends the current timestamp to the log
	 *            file before writing the text message.
	 */
	public static void debug(String text, boolean prependTime) {
		write(_DEBUG, (prependTime ? DateUtil.dateTimeNow() + " " : "") + text);
	}

	/**
	 * Internal method which does all the actual log file writing.
	 * 
	 * @param loglevel
	 *            Log level enumeration to use as the cut off level for loggin
	 * @param text
	 *            Text to write to the log
	 */
	private static synchronized void write(int loglevel, String text) {
		if (loglevel > _loglevel) {
			return;
		}
		writeLog(text,true);
	}

	private static synchronized void writeNoNl(int loglevel, String text) {
		if (loglevel > _loglevel) {
			return;
		}
		writeLog(text,false);
	}

	/**
	 * Internal method which does all the actual log file exception writing.
	 * 
	 * @param loglevel
	 *            Log level enumeration to use as the cut off level for loggin
	 * @param text
	 *            Exception to write to the log
	 */
	private static synchronized void write(int loglevel, Exception e) {
		if (loglevel > _loglevel)
			return;
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			pw.flush();
			sw.flush();
			writeLog(sw.toString(),true);
		} catch (Exception e2) {
		
		}
	}

	/**
	 * Internal method which does all the actual log file exception writing.
	 * 
	 * @param loglevel
	 *            Log level enumeration to use as the cut off level for loggin
	 * @param text
	 *            Exception to write to the log
	 */
	private static synchronized void write(int loglevel, StackTraceElement[] ste) {
		if (loglevel > _loglevel)
			return;
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			for (StackTraceElement ele : ste) {
				pw.write(ele.toString() + "\n");
			}
			pw.flush();
			sw.flush();
			writeLog(sw.toString(), true);
		} catch (Exception e2) {

		}
	}

	private static void writeLog(String text, boolean newline) {
		try {
			if (text == null) {
				return;
			}
			
			if (_out == null) {
				if (_geof_log != null) {
					_out = new BufferedWriter(new FileWriter(_geof_log, _append));
				} else {
					_out = new BufferedWriter(new OutputStreamWriter(System.out));
				}
			}
			if (_write_time) {
				_out.write(DateUtil.dateTimeNow() + " ");
			}
			_out.write(text);
			if (newline) {
				_out.newLine();
			}
			_out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void closeLog() {
		if (_out != null) {
			try {
				_out.close();
			} catch(Exception e){}
		}
	}

	public static void writeInit(String text) {
		try {
			if (text == null || _init_log == null) {
				return;
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(_init_log, true));
			out.write(text);
			out.newLine();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeInitTime() {
		writeInit(DateUtil.dateTimeNow());
		writeInit("");
	}
	
	public static boolean truncate() {
		if ((_geof_log != null) && (_geof_log.exists())) {
			try {
				(new FileWriter(_geof_log)).close();
				info("-------------------------------------");
				info("File truncated at : " + DateUtil.dateTimeNow());
				info("                  ---");
				return true;
			} catch (IOException e) {
				GLogger.error("Logger.truncateFile: " + _geof_log.getName() + " : " + e.getMessage());
			}
		}

		return false;
	}

	/**
	 * 
	 * @return Returns whether or not there is currently an eror in _errorMsg
	 */
	public boolean hasError() {
		return _hasError;
	}

	/**
	 * 
	 * @return Returns the current Error message and then resets the error to
	 *         null
	 */
	public String getError() {
		String errorMsg = _errorMsg;
		_hasError = false;
		_errorMsg = null;
		return errorMsg;
	}

	/**
	 * This method sets the current error
	 * 
	 * @param errorMsg
	 *            New Error message to use
	 */
	protected void setError(String errorMsg) {
		getError();
		_errorMsg = errorMsg;
		_hasError = (_errorMsg != null);
	}

	public static boolean writeTime() {
		return _write_time;
	}
	
	public static void writeTime(boolean write_time) {
		_write_time = write_time;
	}
	
	/**
	 * 
	 * @return Returns whether of not the logger is in Append mode or overwrite.
	 */
	public static boolean getAppend() {
		return _append;
	}

	/**
	 * Sets the write mode of the Logger
	 * 
	 * @param append
	 *            Determines whether the logger will be in Append or Overwrite
	 *            mode
	 */
	public static void setAppend(boolean append) {
		_append = append;
	}
}
