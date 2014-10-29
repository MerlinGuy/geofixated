package org.geof.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.geof.db.ColumnTrio;
import org.geof.encrypt.EncryptableOutputStream;
import org.geof.log.GLogger;
import org.geof.util.ConvertUtil;
import org.geof.util.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.geof.request.Request;

/**
 * The JsonWriter is a Json script specific wrapper around the HTTP response writer used to
 * send responses back to the client for requests. It's main attribute is that responses in
 * the form of Json script do not need to be completed in memory before being serialized out
 * to the requesting client.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class JsonWriter {

	public final static String OPEN_OBJECT = "{";
	public final static String CLOSE_OBJECT = "}";
	public final static String OPEN_ARRAY = "[";
	public final static String CLOSE_ARRAY = "]";
	public final static String DQ = "\"";
	public final static String DQCOLON = "\":";
	public final static String DQCOLONDQ = "\":\"";
	public final static String COMMA = ",";
	public final static String EMPTY = "";
	public static final String PROCTIME = "proctime";

	public boolean _encode = false;
	public String _comma = EMPTY;

	public final static String HEADER = "geofws";
	public final static String ENCRYPTKEY = "encryptkey";
	public final static String TRANSACTION = "transaction";
	public static final String TCALLBACK = "tcallback";
	public final static String SESSION = "session";
	public final static String REQUESTS = "requests";
	public final static String ERROR = "error";
	public static final String SERVER_TIME = "server_time";
	public static final String TID = "tid";

	protected EncryptableOutputStream _outStream = null;
	protected Stack<String> _commaStack = new Stack<String>();
	protected Stack<String> _objectStack = new Stack<String>();
	
	protected boolean _requests_open = false;
	protected Request _request = null;
	protected String _error = null;
	
	/**
	 * Class constructor
	 */
	public JsonWriter() {
	}

	/**
	 * Class constructor
	 * 
	 * @param writer Writer object used to send the response back to the client.
	 */
	public JsonWriter(EncryptableOutputStream stream) {
		_outStream = stream;
	}

	/**
	 * Sets the _encode flag. If encode is true then strings will be Base64 encoded before
	 * written to the response stream.
	 * 
	 * @param encode
	 */
	public void setEncode(boolean encode) {
		_encode = encode;
	}

	/**
	 * Writes a string to the response stream
	 * 
	 * @param value String to be written.
	 */
	public void write(String value) {
		try {
			_outStream.write(value);
		} catch (Exception e) {
			GLogger.error(e);
		}
	}

	/**
	 * This method starts writing the serialized json HTTP response to the user's request.
	 * 
	 * @return Returns true if no error occured, otherwise false
	 */
	public void startWrite(Long curTime, int trans_id) {
		this.write(OPEN_OBJECT);
		_objectStack.push(CLOSE_OBJECT);
		writePair(SERVER_TIME, curTime);
		writePair(TID, trans_id);

	}

	/**
	 * Calls the underlying Writer's flush method
	 */
	public void flush() {
		try {
			_outStream.flush();
		} catch (Exception e) {
			GLogger.error("JsonWriter.flush ", e);
		}
	}

	/**
	 * Sets the _error variable.
	 * 
	 * @param error Text to set the _error to.
	 */
	protected void setError(String error) {
		_error = error;
	}

	/**
	 * Method starts the writing of multiple requests to the response stream
	 * @throws IOException 
	 */
	public void openRequests() throws IOException {
		if (_outStream.canEncrypt()) {
			writeCommaPush();
			writeObjName(REQUESTS);
			openQuote();
			_outStream.encryptOutput(true);			
		} else {
			openArray(REQUESTS);
		}
		_requests_open = true;
	}

	/**
	 * Method writes the closing characters for an open requests block to the response stream
	 * @throws IOException 
	 */
	public void closeRequests() {
		try {
			if (_outStream.isEncrypting()) {
				_outStream.encryptOutput(false);
			}
		} catch (Exception e) {}
		stackPop();
		_requests_open = false;
//		flush();
	}

	public void openRequest(Request r) {
		_request = r;
		writeComma();
		pushComma();
		openObject();
		writePair(Request.ENTITY, r.getEntityName());
		writePair(Request.ACTION, r.getReturnAction());
		writePair(Request.REQUESTID, r.getRequestID());
		String encode = r.getEncode();
		if (encode != null) {
			writePair(Request.ENCODE, encode);
		}
	}
	
	public void closeRequest() {
		if (_request != null) {
			String error = _request.getError();
			if (error != null) {
				writePair(ERROR, error.replaceAll("\"","\'"));
			}
			closeObject();
			_request = null;
		}
	}
	
	/**
	 * This method pops all the remaining closing characters off the _objectStack and writes
	 * them out to the response stream. No other request writes should be made once this is
	 * done.
	 * 
	 * @return
	 */
	protected void completeWrite( Long curTime, String error ) {
		
		if (_request != null) {
			closeRequest();
		}
		if (_requests_open) {
			closeRequests();
		}
		if (error != null) {
			writePair(ERROR, error);
		}

		if (curTime != null) {
			writePair(PROCTIME, (new Date()).getTime() - curTime);
		}

		while (_objectStack.size() > 0) {
			write(_objectStack.pop());
		}
	}

	public boolean writeJsonPair(String name, Object value) {
		write(_comma);
		write(OPEN_OBJECT);
		try {
			write(DQ);
			write(name == null ? "UNKNOWN" : name);
			write(DQCOLONDQ);
			String str = (value == null ? "" : value.toString());
			write(_encode ? ConvertUtil.encode(str) : str);
			write(DQ);
			_comma = COMMA;
		} catch (Exception e) {
			setError("writePair error");
		}
		write(CLOSE_OBJECT);
		return true;
	}
	/**
	 * Writes a Json formatted Name/Value pair to the response stream
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON string to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, String value) {
		try {
			write(_comma);
			write(DQ);
			write(name == null ? "UNKNOWN" : name);
			write(DQCOLONDQ);
			value = (value == null ? "" : value);
			write(_encode ? ConvertUtil.encode(value) : value);
			write(DQ);
			_comma = COMMA;
			return true;
		} catch (Exception e) {
			setError("writePair error");
			return false;
		}
	}

	public boolean writePair(String name, String value, boolean encode) {
		try {
			write(_comma);
			write(DQ);
			write(name == null ? "UNKNOWN" : name);
			write(DQCOLONDQ);
			value = (value == null ? "" : value);
			write(encode ? ConvertUtil.encode(value) : value);
			write(DQ);
			_comma = COMMA;
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			setError("writePair error");
			return false;
		}
	}

	/**
	 * Writes a Json formatted Name/Value pair to the response stream as a literal and does
	 * not wrap the Value parameter in quotes
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON string to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePairNQ(String name, String value) {
		try {
			write(_comma);
			write(DQ);
			write(name == null ? "UNKNOWN" : name);
			write(DQCOLON);
			write(value);
			_comma = COMMA;
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			setError("writePair error");
		}
		return false;
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is an integer
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON integer to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, int value) {
		return writePairNQ(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a double
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON double to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, Double value) {
		return writePairNQ(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a float
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON float to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, Float value) {
		return writePairNQ(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a boolean
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON boolean to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, boolean value) {
		return writePairNQ(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a long
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON long to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, Long value) {
		return writePairNQ(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a Date
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON Date to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, java.sql.Date date) {
		String value = date == null ? "" : DateUtil.DateFormat2.format(date);
		return writePair(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a Sql Time
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON Sql Time to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, java.sql.Time time) {
		return writePair(name, String.valueOf(time));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a Sql Timestamp
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON Sql Timestamp to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, java.sql.Timestamp ts) {
		String value = (ts == null) ? "" : DateUtil.DateFormat2.format(ts);
		return writePair(name, value);
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a JSONObject
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON JSONObject to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, JSONObject jobj) {
		String value = jobj == null ? "" : jobj.toString();
		return writePairNQ(name, String.valueOf(value));
	}

	/**
	 * Writes a Json formatted Name/Value pair where the value is a JSONArray
	 * 
	 * @param name Name of the JSON string object
	 * @param value Value of the JSON JSONArray to write
	 * @return True if no error occurred otherwise false.
	 */
	public boolean writePair(String name, JSONArray jobj) {
		String value = (jobj == null ? "" : jobj.toString());
		return writePairNQ(name, value);
	}

	public boolean writeList(String name, List<?> jobj) {
		String value = "[";
		for (Object jo : jobj) {
			value += "," + jo.toString();
		}
		if (value.length() > 0) {
			value = value.substring(1);
		}
		return writePairNQ(name, value + "]");
	}

/**
	 * Writes a comma to the response stream and also sets the _comma variable.
	 */
	public void writeComma() {
		write(_comma);
		_comma = COMMA;
	}

	/**
	 * Writes a comma to the response stream, sets the _comma variable, and pushes a comma to
	 * the commaStack.
	 */
	public void writeCommaPush() {
		write(_comma);
		_comma = COMMA;
		_commaStack.push(_comma);
		_comma = EMPTY;
	}

	/**
	 * Pushes a comma to the commaStack and sets the _comma object to empty.
	 */
	public void pushComma() {
		_commaStack.push(_comma);
		_comma = EMPTY;
	}

	/**
	 * Pops a comma from the _commaStack if it exists and writes that to the response stream
	 */

	public void popComma() {
		_comma = _commaStack.size() > 0 ? _commaStack.pop() : EMPTY;
	}

	/**
	 * Writes the OPEN string to response stream and places the CLOSE string on the
	 * _objectStack.
	 */

	public void writeOpen() {
		write(OPEN_OBJECT);
		stackPush(CLOSE_OBJECT);
	}

	/**
	 * Writes the OPENBRACK string to response stream and places the CLOSEBRACK string on the
	 * _objectStack.
	 */
	public void openBracket() {
		write(OPEN_ARRAY);
		stackPush(CLOSE_ARRAY);
	}

	public void openQuote() {
		write(DQ);
		stackPush(DQ);
	}
	/**
	 * Method starts writing a new named JSON object to the response stream
	 * 
	 * @param name Name of the new JSON Object to write.
	 */

	public void writeObjName(String name) {
		write(DQ);
		write(name == null ? "UNKNOWN" : name);
		write(DQCOLON);
	}

	/**
	 * Writes a String value to the response stream.
	 * 
	 * @return True if no error occurs otherwise false
	 */

	/****************************************************/
	public boolean writeValue(String value) {
		try {
			writeComma();
			write(DQ);
			value = (value == null ? "" : value);
			write(_encode ? ConvertUtil.encode(value) : value);
			write(DQ);
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			setError("writeValue error");
			return false;
		}
	}

	/**
	 * Writes a non-quoted wrapped String to the response stream
	 * 
	 * @param value String to write the response stream
	 * @return Always returns true
	 */

	public boolean writeValueNQ(String value) {
		writeComma();
		write(value);
		return true;
	}

	/**
	 * Writes a integer to the response stream
	 * 
	 * @param value Integer to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */
	public boolean writeValue(int value) {
		return writeValueNQ(String.valueOf(value));
	}

	/**
	 * Writes a double to the response stream
	 * 
	 * @param value Double to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */
	public boolean writeValue(Double value) {
		return writeValueNQ(String.valueOf(value));
	}

	/**
	 * Writes a float to the response stream
	 * 
	 * @param value Float to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */

	public boolean writeValue(Float value) {
		return writeValueNQ(String.valueOf(value));
	}

	/**
	 * Writes a boolean to the response stream
	 * 
	 * @param value Boolean to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */
	public boolean writeValue(boolean value) {
		return writeValueNQ(String.valueOf(value));
	}

	/**
	 * Writes a long to the response stream
	 * 
	 * @param value Long to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */

	public boolean writeValue(Long value) {
		return writeValueNQ(String.valueOf(value));
	}

	/**
	 * Writes a Sql Date to the response stream
	 * 
	 * @param value Sql Date to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */
	public boolean writeValue(java.sql.Date date) {
		String value = date == null ? "" : DateUtil.DateFormat2.format(date);
		return writeValue(String.valueOf(value));
	}

	/**
	 * Writes a Sql Time to the response stream
	 * 
	 * @param value Sql Time to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */

	public boolean writeValue(java.sql.Time time) {
		return writeValue(String.valueOf(time));
	}

	/**
	 * Writes a Sql Timestamp to the response stream
	 * 
	 * @param value Sql Timestamp to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */
	public boolean writeValue(java.sql.Timestamp ts) {
		String value = (ts == null) ? "" : DateUtil.DateFormat2.format(ts);
		return writeValue(value);
	}

	/**
	 * Writes a JSONObject to the response stream
	 * 
	 * @param value JSONObject to write the response stream
	 * @return Returns the value returned by writeValueNQ
	 */

	public boolean writeValue(JSONObject jobj) {
		String value = jobj == null ? "" : jobj.toString();
		return writeValueNQ(String.valueOf(value));
	}

	/**
	 * Writes the JSON script format object start text to the response stream
	 * 
	 * @param name Name of the JSONObject to write
	 */

	public void openObject(String name) {
		writeCommaPush();
		write(DQ);
		write(name == null ? "UNKNOWN" : name);
		write(DQCOLON);
		writeOpen();
	}

	/**
	 * Starts the writing of an JSONObject to the response stream
	 */

	public void openObject() {
		writeCommaPush();
		writeOpen();
	}

	/**
	 * Writes the closing text for an JSONObject to the response stream
	 */
	public void closeObject() {
		stackPop();
	}

	/**
	 * Writes the JSON script format for an Array object's start text to the response stream
	 * 
	 * @param name Name of the JSONArray to write
	 */

	public void openArray(String name) {
		writeCommaPush();
		writeObjName(name);
		openBracket();
	}

	/**
	 * Writes the closing text for an JSONArray to the response stream
	 */
	public void closeArray() {
		stackPop();
	}

	/**
	 * Writes the a serialized JSONArray to the response stream
	 * 
	 * @param name Name of the array to write
	 * @param array JSONArray object to serialize to response stream
	 */
	public void writeArray(String name, JSONArray array) {
		writeCommaPush();
		writeObjName(name);
		write(array.toString());
	}

	/**
	 * Writes the a string list to the response stream as an Array
	 * 
	 * @param name Name of the array to write
	 * @param stringList String list to write
	 */
	public void writeArray(String name, String stringList) {
		writeCommaPush();
		writeObjName(name);
		openBracket();
		write(stringList);
		stackPop();
	}

	/**
	 * Pushes a string on the _objectStack to be written out later
	 * 
	 * @param value String value to place on the _objectStack
	 */

	public void stackPush(String value) {
		_objectStack.push(value);
	}

	/**
	 * Pops a value off the _objectStack and writes it to the response stream
	 */

	public void stackPop() {
		if (_objectStack.size() > 0) {
			write(_objectStack.pop());
		}
		popComma();
//		flush();
	}

	public void writeResultset(ResultSet rs) throws Exception {
		writeResultset(rs, ColumnTrio.getColumns(rs.getMetaData()));
	}	

	/**
	 * Writes a sql ResultSet object out the response stream as an JSONArray
	 * 
	 * @param arrayname Name of the ResultSet
	 * @param rs ResultSet object to write out the response stream
	 * @param flds List of fields to write to the response stream
	 * @return True if no error occurs
	 */

	public void writeResultset(String arrayname, ResultSet rs, String[] flds) throws Exception {
		openArray(arrayname);
		try {
			writeResultset(rs, flds);
		} catch (Exception e) {
			throw e;
		} finally {
			closeArray();
			_comma = COMMA;
		}
	}

	/**
	 * Writes a sql ResultSet object out the response stream as an unnamed JSONArray
	 * 
	 * @param rs ResultSet object to write out the response stream
	 * @param flds List of fields to write to the response stream
	 * @param leaveopen If True then the JSONObject close text will not be written so that
	 * more information can be written later.
	 * @return True if no error occurs
	 */

	public void writeResultset(ResultSet rs, String[] flds) throws Exception {
		if (rs != null) {
			ColumnTrio[] ct = ColumnTrio.getList(rs.getMetaData(), flds);
			while (rs.next()) {
				writeRecord(rs, ct);
			}
		}
	}
	
	/**
	 * Writes a sql ResultSet object out the response stream as an unnamed JSONArray.
	 * 
	 * @param rs ResultSet object to write out the response stream
	 * @param flds List of fields to write to the response stream
	 * @param leaveopen If True then the JSONObject close text will not be written so that
	 * more information can be written later.
	 * @param rsmd ResultSetMetaData to use for generating the ColumnTrio list if no cps is
	 * passed in.
	 * @param cps Array of ColumnTrio objects to use for deteriming Resultset column datatypes
	 * @return True if no error occurs
	 */

	public boolean writeResultset(ResultSet rs, String[] fields, ColumnTrio[] cps) {
		try {
			openObject();
			for (ColumnTrio cp : cps) {
				if (cp.Datatype == java.sql.Types.CHAR) {
					writeValue(rs.getString(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.VARCHAR) {
					writePair(cp.FieldName, rs.getString(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.LONGVARCHAR) {
					writePair(cp.FieldName, rs.getString(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.NUMERIC) {
					writePair(cp.FieldName, rs.getDouble(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.DECIMAL) {
					writePair(cp.FieldName, rs.getDouble(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.BIT) {
					writePair(cp.FieldName, rs.getBoolean(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.TINYINT) {
					writePair(cp.FieldName, rs.getByte(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.SMALLINT) {
					writePair(cp.FieldName, rs.getShort(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.INTEGER) {
					writePair(cp.FieldName, rs.getInt(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.BIGINT) {
					writePair(cp.FieldName, rs.getLong(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.REAL) {
					writePair(cp.FieldName, rs.getFloat(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.FLOAT) {
					writePair(cp.FieldName, rs.getFloat(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.DOUBLE) {
					writePair(cp.FieldName, rs.getDouble(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.DATE) {
					writePair(cp.FieldName, rs.getDate(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.TIME) {
					writePair(cp.FieldName, rs.getTime(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.TIMESTAMP) {
					writePair(cp.FieldName, rs.getTimestamp(cp.Indx));
				} else {
					writePair(cp.FieldName, rs.getString(cp.Indx));
				} 
			}
			closeObject();
			return true;
		} catch (Exception e) {
			GLogger.error("writeRecord ", e);
			return false;
		}
	}
	
	public boolean writeResultSetEncoded(ResultSet rs, String[] encoded) {
		try {
			return writeRecordset(rs, ColumnTrio.getEncodedList(rs.getMetaData(), encoded));
		} catch (SQLException e) {
			GLogger.error("writeRecordset ", e);
			return false;
		}
	}
	
	public boolean writeRecordset(ResultSet rs, ColumnTrio[] list) {
		try {
			while (rs.next()) {
				writeRecord(rs, list);
			}
			return true;
		} catch (Exception e) {
			GLogger.error("writeRecordset ", e);
			return false;
		}
	}

	public boolean writeRecord(ResultSet rs, ColumnTrio[] cps) {
		openObject();
		try {
			for (ColumnTrio cp : cps) {
				if (cp.Datatype == java.sql.Types.CHAR) {
					writeValue(rs.getString(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.VARCHAR) {
					writePair(cp.FieldName, rs.getString(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.LONGVARCHAR) {
					writePair(cp.FieldName, rs.getString(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.NUMERIC) {
					writePair(cp.FieldName, rs.getDouble(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.DECIMAL) {
					writePair(cp.FieldName, rs.getDouble(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.BIT) {
					writePair(cp.FieldName, rs.getBoolean(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.TINYINT) {
					writePair(cp.FieldName, rs.getByte(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.SMALLINT) {
					writePair(cp.FieldName, rs.getShort(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.INTEGER) {
					writePair(cp.FieldName, rs.getInt(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.BIGINT) {
					writePair(cp.FieldName, rs.getLong(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.REAL) {
					writePair(cp.FieldName, rs.getFloat(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.FLOAT) {
					writePair(cp.FieldName, rs.getFloat(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.DOUBLE) {
					writePair(cp.FieldName, rs.getDouble(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.DATE) {
					writePair(cp.FieldName, rs.getDate(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.TIME) {
					writePair(cp.FieldName, rs.getTime(cp.Indx));
				} else if (cp.Datatype == java.sql.Types.TIMESTAMP) {
					writePair(cp.FieldName, rs.getTimestamp(cp.Indx));
				} else {
					writePair(cp.FieldName, rs.getString(cp.Indx));
				} 
			}
			return true;
		} catch (Exception e) {
			GLogger.error("writeRecord ", e);
			return false;
		} finally {
			closeObject();
		}
	}



	/**
	 * Clears any in memory objects for garbage collection
	 */

	public void dispose() {
		if (_outStream != null) {
			try {
				_outStream.close();
			} catch (Exception e) {}
			_outStream = null;
		}
		_commaStack.clear();
		_commaStack = null;
		_objectStack.clear();
		_objectStack = null;
	}

	/**
	 * Writes the contents of the _objectStack to the Log file for debugging purpose.
	 */

	public void DebugStack() {
		int count = _objectStack.size() - 1;
		StringBuilder sb = new StringBuilder();
		for (int indx = count; indx > -1; indx--) {
			sb.append(_objectStack.get(indx));
		}
		GLogger.error(sb.toString());
	}

	/**
	 * Writes the contents of the _objectStack to the Log file with text as a prefix for debugging purpose.
	 * @param text  Prefix to write to the log file
	 */

	public void DebugStack(String text) {
		int count = _objectStack.size() - 1;
		StringBuilder sb = new StringBuilder();
		sb.append(text);
		for (int indx = count; indx > -1; indx--) {
			sb.append(_objectStack.get(indx));
		}
		GLogger.error(sb.toString());
	}
	
}
