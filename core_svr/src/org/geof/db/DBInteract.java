package org.geof.db;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import org.geof.log.GLogger;
import org.geof.request.Request;
import org.geof.service.GSession;
import org.geof.service.JsonWriter;
import org.geof.service.Transaction;
import org.geof.util.DateUtil;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class performs all the database interaction work between org.geof.request objects,
 * the relational database and outputs the results to HTTP response stream via the JsonWriter object.  
 * 
 * @author Jeff Boehmer
 * @comanpay  Ft Collins Research, LLC.
 * @url	www.ftcollinsresearch.org
 *
 */
public class DBInteract {

	public static final int IDONLY = 0, FULLOBJECT = 1;

	public static final String SKIP = "skip";
	public static final String CHILDREN = "children";
	public static final String LINKS = "links";
	public static final String TIE = "tie";
	public static final String FIELDS = "fields";
	public static final String COLUMNS = "columns";
	public static final String WHERE = "where";
	public static final String LINK = "link";
	public static final String JOIN = "join";
	public static final String ID = "id";
	public static final String DATA = "data";
	public static final String COMMA = "comma";
	public final static String ORDERBY = "orderby";
	public final static String AND = " AND ";
	public final static String OR = " OR ";
	public final static String ALL = "all";
	public final static String TYPE = "type";
	public final static String ENCODE = "encode";
	public final static String REGISTEREDBY = "registeredby";
	public final static String CREATEDBY = "createdby";

	public String LINK_KEYWORD = "_keyword";
	public final String[] KEYWORDFIELDS = { "id", "keyword" };
	public final static String ENTITY = "entity";

	/*
	 * - All Fields - Default Fields - Specific fields - Tables - Joins - Constraints - Group
	 * By - Order by
	 */
	private String _lastSQL = null;  // Stores that last SQL query executed 
	private JSONObject _lastReturning = null;  // Stores the primary key fields of the last row returned from the database
	protected String _error = null;  
	protected DBConnection _conn = null;
	protected Transaction _transaction = null;
	protected boolean _hasOwnDbConnection = false;

	protected JsonWriter _jwriter = null;

	public DBInteract(Transaction transaction) {
		this._transaction = transaction;
		this._conn = transaction.getConnection();
		this._jwriter = transaction.getStream();
	}

	/**
	 * Class constructor
	 * @param conn  DBConnection object to use for all queries.
	 * @param writer  Output stream to write all return row or error information
	 */
	public DBInteract(DBConnection conn, JsonWriter writer) {
		_conn = conn;
		_jwriter = writer;
	}

	public DBInteract() {
		_conn = DBConnMgr.getConnection();
		_jwriter = null;
	}
	
	public void releaseConnection() {
		DBConnMgr.release(_conn);
		_conn = null;
	}
	
	public long getConnectionId() {
		return _conn.getID();
	}
	
	public boolean spawnDbConnection() {
		_hasOwnDbConnection = false;
		if (_conn != null) {
			GSession gsession = _conn.getSession();
			if (gsession != null) {
				_conn = DBConnMgr.getConnection(gsession);
				_hasOwnDbConnection = true;
				return true;
			}			
		}
		return false;
	}
	
	public long getUsrId() {
		return (_conn != null) ? _conn.getUsrID() : -1;
	}
	
	public JSONObject getUsr() throws Exception {
		long usrid = getUsrId();
		JSONObject usrData = JsonUtil.getDataWhere(Request.ID, usrid);
		JSONArray jaUsers = readAsJson(EntityMapMgr.getEMap("usr"), usrData);
		if ( jaUsers.length() != 1) {
			throw new Exception("Could not retrieve user for usrid:" + usrid);
		}
		return jaUsers.getJSONObject(0);
	}

	public Transaction getTransaction() {
		return _transaction;
	}

	public void clearSession() {
		_conn.setSession(null);
	}
	
	public DatabaseMetaData getDbMetaData() {
		return (_conn == null) ? null : _conn.getMetaData();
	}
	/**
	 *  
	 * @return  the last SQL run against the database if available
	 */
	public String getLastSQL() {
		return _lastSQL;
	}

	public void setLastSQL (String sql) {
		_lastSQL = sql;
	}
	
	/**
	 * Method creates a database record in the table reference by the EntityMap object 
	 * using the field data in JSONObject data.
	 * @param emap  Entity to Table mapping object
	 * @param data  JsonObject holding all row data as well as link and child information
	 * @param useReturning  Determines whether the primary key information should be placed
	 * in the _lastReturning Object after the database Insert for later reference.  Usually 
	 * this is used as a foreign key into link and child records
	 * @return  True if the creation was sucessfull, false if an error occured.
	 */
	public boolean create(EntityMap emap, JSONObject data) {
		try {

			JSONObject fields = data.getJSONObject(FIELDS);
			fields.put(CREATEDBY, _conn.getUsrID());
			fields.put(REGISTEREDBY, _conn.getUsrID());
			
			ArrayList<String> required = emap.getRequiredFldsAsList();
			ArrayList<String> exclude = emap.getAutoFldsAsList();
			ParameterList params = new ParameterList(emap, fields, exclude, required);

			String entityname = emap.getName();

			String[] pkeys = emap.getPrimaryKeys();
			PreparedStatement ps = buildCreate(entityname, params, pkeys);
			boolean isReturning = ((pkeys != null) && (pkeys.length > 0));

			if ((ps != null) && (isReturning)) {
				// rs holds the returning data from the create statement
				ResultSet rs = getResultSetReturning(ps, params, null);

				_lastReturning = getReturning(rs);
				if (_jwriter != null) {
					_jwriter.writePair("pkey", _lastReturning);
				}

			} else {
				executeStatement(ps, params, null);
			}
			return true;
		} catch (Exception e) {
			return setError(e);
		}
	}

	/**
	 * Method creates a database record in the table reference by the EntityMap object 
	 * using the field data in JSONObject data.
	 * @param emap  Entity to Table mapping object
	 * @param data  JsonObject holding all row data as well as link and child information
	 * @param useReturning  Determines whether the primary key information should be placed
	 * in the _lastReturning Object after the database Insert for later reference.  Usually 
	 * this is used as a foreign key into link and child records
	 * @return  True if the creation was sucessfull, false if an error occured.
	 * @throws Exception 
	 */
	public JSONObject create(String entityname, JSONObject data) throws Exception {
		EntityMap emap = EntityMapMgr.getEMap(entityname);
		JSONObject fields = data.getJSONObject(FIELDS);
		fields.put(CREATEDBY, _conn.getUsrID());
		fields.put(REGISTEREDBY, _conn.getUsrID());
		
		ArrayList<String> required = emap.getRequiredFldsAsList();
		ArrayList<String> exclude = emap.getAutoFldsAsList();
		ParameterList params = new ParameterList(emap, fields, exclude, required);
		PreparedStatement ps = buildCreate(entityname, params, emap.getPrimaryKeys());
		ResultSet rs = getResultSetReturning(ps, params, null);
   		return getReturning(rs);
	}

	/**
	 * Updates existing records with new field data.
	 * @param emap  Entity to Table mapping object
	 * @param data  JsonObject holding all row data as well as link and child information
	 * @param useReturning  Determines whether the primary key information should be placed
	 * in the _lastReturning Object after the database Insert for later reference.  Usually 
	 * this is used as a foreign key into link and child records
	 * @return  True if the creation was sucessfull, false if an error occured.
	 */
	public boolean update(EntityMap emap, JSONObject data) {
		try {
			JSONObject fields = JsonUtil.getJsonObject(data, FIELDS, null);
			if (fields == null) {
				return setError("DBInteract.update failed data object missing 'fields' element");
			}

			JSONObject where = JsonUtil.getJsonObject(data, WHERE, null);
			if (where == null) {
				return setError("DBInteract.update failed data object missing 'where' element");
			}

			ArrayList<String> exclude = emap.getAutoFldsAsList();
			ParameterList fldParams = new ParameterList(emap, fields, exclude, null);
			ParameterList whereParams = new ParameterList(emap, where, null, null);
			String entityname = emap.getName();

			String[] pkeys = emap.getPrimaryKeys();
			PreparedStatement ps = buildUpdate(entityname, fldParams, whereParams, pkeys);
			if (ps == null) {
				return setError("DBInteract Error:  buildUpdate returned null prepared statement");
			}
			executeStatement(ps, fldParams, whereParams);
			return true;
		} catch (Exception e) {
			return setError(e);
		}
	}


	/**
	 * Deletes the record in the Table specified by the EntityMap object using 
	 * the constraint information in the where object
	 * 
	 * @param emp  Entity to Table mapping object
	 * @param where  Constraint field and value object
	 * @param cascadeDelete  If true deletes all links and children objects
	 * @return  True if the delete was sucessfull otherwise false.
	 */
	public boolean delete(EntityMap emap, JSONObject where) {
		boolean rtn = false;
		try {
			ParameterList whereParams = new ParameterList(emap, where, null, null);
			String[] pkeys = emap.getPrimaryKeys();
			String entityName = emap.getName();
			PreparedStatement ps = buildDelete(entityName, whereParams, pkeys);
			if (ps != null) {
				rtn = executeStatement(ps, whereParams, null);
				long id = where.optLong( "id", -1);
				if (id != -1) {
					deleteLinks(entityName, id);
					deleteChildren(entityName, id);					
				}
			}
		} catch (Exception e) {
			GLogger.error(_lastSQL);
			return setError(e);
		} finally {
			_conn.closeAll();
		}
		return rtn;
	}
		
	public boolean deleteLinks(String tblName, long id) {
		try {
			ArrayList<EntityMap> emaps = EntityMapMgr.getLinkEmaps(tblName);
			String sql = null;
			for (EntityMap emap : emaps) {
				sql = "DELETE FROM " + emap.getName() + " WHERE " + tblName + "id = ?";
				PreparedStatement ps = _conn.getPreparedStatement(sql);
				ps.setLong(1, id);
				ps.execute();
			}		
			return true;
		} catch (Exception e) {
			return setError("DBInteract.removeLinks: " + e.getMessage());
		}
	}

	public boolean addLink(String tblFrom, long fromID, String tblTo, long toID) {
		try {
			EntityMap emap = EntityMapMgr.getLinkMap(tblFrom, tblTo);
			if (emap == null) {
				this.setError("Missing Entity map for link tables: " + tblFrom + "," + tblTo);
				return false;
			}
			String tbl = emap.getName();
			String fromKey = tblFrom + "id";
			String toKey = tblTo + "id";
			String sql = "DELETE FROM " + tbl + " WHERE " + fromKey + "=? AND " + toKey + "=?";
			PreparedStatement ps = _conn.getPreparedStatement(sql);
			ps.setLong(1,fromID);
			ps.setLong(2,toID);
			ps.execute();
			sql = "INSERT INTO " + tbl + " (" + fromKey + "," + toKey + ") VALUES (?,?)";
			ps = _conn.getPreparedStatement(sql);
			ps.setLong(1,fromID);
			ps.setLong(2,toID);
			ps.execute();
			return true;
		} catch (Exception e) {
			return setError("DBInteract.addLink: " + e.getMessage());
		}
	}
	
	public JSONArray getLinks(String tblFrom, long fromID, String tblTo) {
		try {
			EntityMap emap = EntityMapMgr.getLinkMap(tblFrom, tblTo);
			if (emap == null) {
				setError("Missing Entity map for link tables: " + tblFrom + "," + tblTo);
				return null;
			}
			String tbl = emap.getName();
			String fromKey = tblFrom + "id";
			String sql = "SELECT * FROM " + tbl + " WHERE " + fromKey + "=?";
			PreparedStatement ps = _conn.getPreparedStatement(sql);
			ps.setLong(1,fromID);
			JSONArray ja = this.readAsJson(ps, sql);
			ps.close();
			return ja;
		} catch (Exception e) {
			setError("DBInteract.getLinks: " + e.getMessage());
			return null;
		}
	}
	
	public void deleteChildren(String tblParent, long id) {
		String tbl = null;	
		try {
			ArrayList<EntityMap> childEmaps = EntityMapMgr.getChildEmaps(tblParent);
					
			// Now remove any orphaned records
			PreparedStatement ps = null;
			for (EntityMap lemap : childEmaps) {
				tbl = lemap.getName();
				String sql = "DELETE FROM " + tbl + " WHERE " + tblParent + "id = ?";
				ps = _conn.getPreparedStatement(sql);
				ps.setLong(1, id);
				ps.execute();
			}
		} catch (Exception e) {
			setError("DBInteract.deleteChildren - parent:" + tblParent + " child: " + tbl + " error: " + e.getMessage());
		}
	}

	/**
	 * Returns the record or records from the table specified in the emap object
	 * @param emap  Entity to Table mapping object
	 * @param data  Holds constraint information for the select query
	 * @return  True if the read succeeds otherwise false
	 */
	public void read(EntityMap emap, JSONObject data) throws Exception {
		if (data.has(JOIN)) {
			readComplex(emap, data);
		} else {
			read(DATA, emap, data) ;
		}
	}

	/**
	 * Returns the record or records from the table specified in the emap object
	 * @param emap  Entity to Table mapping object
	 * @param data  Holds constraint information for the select query
	 * @param leaveOpen  Determines whether the return stream object is closed in the
	 * JsonWriter or left open for later additional information writes
	 * @return  True if the read succeeds otherwise false
	 */
	public boolean read(String ArrayName, EntityMap emap, JSONObject data) throws Exception {
		boolean rtn = false;
		if (data == null) {
			data = new JSONObject();
		}
		JSONObject where = JsonUtil.getJsonObject(data, WHERE, null);
		ParameterList pl = null;

		String tbl = emap.getName();

		String[] columns = null;
		String columnsCSV = data.optString( COLUMNS, "");

		if (columnsCSV.length() > 0) {
			columns = columnsCSV.split(",");
		} else {
			columns = emap.getDefaultFields();
		}
		columnsCSV = emap.getFieldsCsv(columns, false);

		String orderby = data.optString("orderby", null);
		
		if (where != null) {
			pl = new ParameterList(emap, where);
		}
		PreparedStatement ps = buildRead(tbl, columnsCSV, pl, orderby);
		ResultSet rs = ps.executeQuery();
		
		if (rs == null) {
			GLogger.error("read() resultset null");
			return false;
		}
		_jwriter.openArray(ArrayName); 
		_jwriter.writeResultset(rs, columns);
		_jwriter.closeArray(); //Close DATA array
		return rtn;
	}

	public void readComplex(EntityMap emap, JSONObject data) throws Exception {
		PreparedStatement ps = QueryBuilder.getComplexRead(emap, data,  this);
		ResultSet rs = ps.executeQuery();
		_jwriter.openArray(DATA); 
		try {
			_jwriter.writeResultset(rs);
		} catch (Exception e) {
			throw e;
		} finally {
			_jwriter.closeArray(); //Close DATA array
		}
	}
	
	public void read(PreparedStatement ps) throws Exception {
		_jwriter.openArray(DATA); 
		ResultSet rs = ps.executeQuery();			
		try {
			_jwriter.writeResultset(rs);
		} catch (Exception e) {
			throw e;
		} finally {
			_jwriter.closeArray();
			close(rs);
		}
	}

	public JSONArray readAsJson( EntityMap emap, JSONObject data) throws Exception {
		return JsonUtil.toJSONArray(readRecords(emap, data));
	}
	
	public JSONArray readAsJson( String emapName, JSONObject data) throws Exception {
		return readAsJson( EntityMapMgr.getEMap(emapName), data );
	}
	
	public JSONArray readAsJson( String sql, ParameterList pl) throws Exception {
		_lastSQL = sql;
		PreparedStatement ps = _conn.getPreparedStatement(sql);
		if (pl != null) {
			pl.setPreparedStatement(ps);
		}
		ResultSet rs = ps.executeQuery();
		JSONArray ja = JsonUtil.toJSONArray(rs);
		rs.close();
		ps.close();
		return ja;
	}
	
	public JSONArray readAsJson(PreparedStatement ps, String sql) throws Exception {
		_lastSQL = sql;
		return JsonUtil.toJSONArray(ps.executeQuery());
	}
	
	public ResultSet readRecords(EntityMap emap, JSONObject data) throws Exception {
		if (data == null) {
			data = new JSONObject();
		}
		String columnsCSV = emap.fieldsCsv(data.optString(COLUMNS, null), null);

		ParameterList pl = null;
		JSONObject where = JsonUtil.getJsonObject(data, WHERE, null);
		if (where != null) {
			pl = new ParameterList(emap, where);
		}

		String orderby = QueryBuilder.parseOrderBy(data);
		PreparedStatement ps = buildRead(emap.getName(), columnsCSV, pl, orderby);
		return ps.executeQuery();
	}

	/**
	 * Extracts the Where Statement object from the passed in Data object
	 * @param emap  Entity to Table mapping object
	 * @param data  Data object which holds the Where object
	 * @return  JSONObject containing a formatted where object
	 */
	public JSONObject getWhere(EntityMap emap, JSONObject data) {

		try {
			if (data.has(WHERE)) {
				return data.getJSONObject(WHERE);
			}
			if (!data.has(FIELDS)) {
				GLogger.error("DBInteract.getWhere failed, missing {fields{}}: ");
				return null;
			}
			JSONObject fields = data.getJSONObject(FIELDS);
			String[] pkeys = emap.getPrimaryKeys();
			JSONObject where = new JSONObject();
			for (String key : pkeys) {
				if (fields.has(key)) {
					where.put(key, fields.get(key));
				} else {
					GLogger.error("DBInteract.getWhere failed, missing pkey: " + key);
					return null;
				}
			}
			return where;
		} catch (JSONException e) {
			setError(e);
			return null;
		}
	}

	/**
	 * Executes a PreparedStatement 
	 * @param ps  Prepared Statement to execute against the database
	 * @param pl1 Optional list of Name/Value pairs to use as where constraints
	 * @param pl2 Second optional list of Name/Value pairs to use as where constraints
	 * @return True if the execute succeeds otherwise false.
	 */
	public boolean executeStatement(PreparedStatement ps, ParameterList pl1, ParameterList pl2) {
		try {
			ps.clearParameters();
			if (pl1 != null) {
				pl1.setPreparedStatement(ps);
			}

			// secondary param set (usually a where constraint set)
			if (pl2 != null) {
				int offset = (pl1 == null) ? 0 : pl1.getParamCount();
				pl2.setPreparedStatement(ps, offset);
			}
			ps.execute();
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			GLogger.error("DBRequest.executeStatement ERROR: " + e.getMessage());
			GLogger.error("_lastSQL " + _lastSQL);
			if (pl1 != null) {
				GLogger.error("pl1: " + pl1.toString());
			}
			if (pl2 != null) {
				GLogger.error("pl2: " + pl2.toString());
			}
			return false;
		}
	}

	/**
	 * Executes the SQL string using the constaints listed in the ParameterList
	 * @param sql  SQL query to execute
	 * @param pl  ParameterList containing Name/Value pairs to use as the where contraint
	 * @return  True if the query is successful otherwise false
	 */
	public boolean executePL(String sql, ParameterList pl) throws Exception {
		PreparedStatement ps = null;
		_lastSQL = sql;
		if (pl != null) {
			ps = _conn.getPreparedStatement(_lastSQL);
			pl.setPreparedStatement(ps);
			ps.execute();
			close(ps);
		} else {
			_conn.executeQuery(_lastSQL);
		}
		return true;
	}

	public boolean executeLR(String sql, PreparedStatement ps) {
		ResultSet rs = null;
		try {
			_lastSQL = sql;
			rs =  ps.executeQuery();
			_lastReturning = getReturning(rs);
			return true;
		} catch (Exception e) {
			return setError(e);
		} finally {
			close(rs);
		}
	}

	public boolean execute(String sql) {
		PreparedStatement ps = null;
		try {
			_lastSQL = sql;
			ps = getPreparedStatement(_lastSQL);
			return ps.execute();
		} catch (SQLException e) {
			_error = e.getMessage();
			return false;
		} finally {
			close(ps);
		}
	}
	
	public static void close(PreparedStatement ps) {
		if (ps != null) {
			try {ps.close();} catch (SQLException e) {}
		}		
	}
	public static void close(ResultSet rs) {
		if (rs != null) {
			try {rs.close();} catch (SQLException e) {}
		}		
	}

	/**
	 * Executes the SQL string using the constaints listed in the ParameterList
	 * and returns the resultset.
	 * @param sql  SQL query to execute
	 * @param pl  ParameterList containing Name/Value pairs to use as the where contraint
	 * @return  ResultSet if the query is successful otherwise null;
	 */
	public ResultSet getResultSet(String sql, ParameterList pl) throws Exception {
		_lastSQL = sql;
		if (pl != null) {
			PreparedStatement ps = _conn.getPreparedStatement(_lastSQL);
			pl.setPreparedStatement(ps);
			return ps.executeQuery();
		} else {
			return _conn.getResultset(sql);
		}
	}

	public ResultSet executePS(String sql, PreparedStatement ps) {
		try {
			_lastSQL = sql;
			return ps.executeQuery();
		} catch (SQLException e) {
			setError(e);
			return null;
		} 
	}

	public PreparedStatement getPreparedStatement(String sql) {
		return _conn.getPreparedStatement(sql);
	}

	/**
	 * Executes the Prepared Statement using the constaints listed in the ParameterLists
	 * and returns the special returning fields denoted in the Prepared statement.
	 * @param ps  Prepared Statement executed against the Database
	 * @param pl1 optional list of Name/Value pairs to use as where constraints
	 * @param pl2 Second optional list of Name/Value pairs to use as where constraints
	 * @return  ResultSet which generated as the Returning fields of the SQL query.
	 */
	public ResultSet getResultSetReturning(PreparedStatement ps, ParameterList pl1, ParameterList pl2) throws Exception {
		ps.clearParameters();
		if (pl1 != null) {
			pl1.setPreparedStatement(ps);
		}

		// secondary param set (usually a where constraint set)
		if (pl2 != null) {
			int offset = (pl1 == null) ? 0 : pl1.getParamCount();
			pl2.setPreparedStatement(ps, offset);
		}
		return ps.executeQuery();
	}

	/**
	 * Method builds a create based Prepared Statement referencing the table denoted by the tbl field
	 * and the contraints listed in the ParameterList.
	 * @param tbl Table name to build the INSERT statement from
	 * @param pl  ParameterList containing the constraint fields to use in building the Prepared Statement.
	 * @param Optional returning fields to add to the end of the Prepared Statement.
	 * @return Resultant Prepared Statement
	 */
	private PreparedStatement buildCreate(String tbl, ParameterList pl, String[] returning) {

		StringBuilder sb = new StringBuilder("INSERT INTO ");
		sb.append(tbl).append(" (");
		StringBuilder flds = new StringBuilder();
		StringBuilder values = new StringBuilder();

		int fieldcount = pl.getParamCount();
		for (int indx = 0; indx < fieldcount; indx++) {
			flds.append(",").append(pl.getParamName(indx));
			values.append(",?");
		}
		sb.append(flds.substring(1)).append(") VALUES (").append(values.substring(1)).append(")");

		if ((returning != null) && (returning.length > 0)) {
			sb.append(" RETURNING ");
			for (int I = 0; I < returning.length; I++) {
				if (I > 0) {
					sb.append(",");
				}
				sb.append(returning[I]).append(" AS ").append(returning[I]);
			}
		}
		_lastSQL = sb.toString();
		return _conn.getPreparedStatement(_lastSQL);
	}

	/**
	 * Builds a PreparedStatement for reading records from the database
	 * @param tbl  Table name to base the Select statement on
	 * @param flds  Comma delimited list of field names to return in the query
	 * @param pl  Contraint fields to add to the where statement section of the quest
	 * @param orderby  Comma delimited string of order by fields
	 * @return  Returns the built PreparedStatement.
	 */
	protected PreparedStatement buildRead(String tbl, String flds, ParameterList pl, String orderby) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ").append(flds).append(" FROM ").append(tbl);
		if ((pl != null) && (pl.getParamCount() > 0)) {
			sb.append(" WHERE ");
			sb.append(pl.getWhere(false));
		}
		if ((orderby != null) && (orderby.length() > 0)) {
			sb.append(" ORDER BY ").append(orderby);
		}
		_lastSQL = sb.toString();
		PreparedStatement ps = _conn.getPreparedStatement(_lastSQL);
		if (pl != null) {
			pl.setPreparedStatement(ps);
		}
		return ps;
	}

	/**
	 * Builds a PreparedStatement for updating records in the database
	 * @param tbl  Table to update in database
	 * @param pl1 Parameter List of Name/Values for fields to update
	 * @param where  Parameter List of constraint fields for where section
	 * @param returning  Fields to return from the update query
	 * @return Returns a PreparedStatement if successful otherwise null if an error occurs
	 */
	protected PreparedStatement buildUpdate(String tbl, ParameterList pl1, ParameterList where, String[] returning) {

		StringBuilder sb = (new StringBuilder("UPDATE ")).append(tbl).append(" SET ");
		String comma = "";
		int fieldcount = pl1.getParamCount();
		for (int indx = 0; indx < fieldcount; indx++) {
			sb.append(comma).append(pl1.getParamName(indx)).append(" = ?");
			comma = ",";
		}
		if (where != null) {
			sb.append(" WHERE ");
			comma = "";
			fieldcount = where.getParamCount();
			for (int indx = 0; indx < fieldcount; indx++) {
				sb.append(comma).append(where.getParamName(indx)).append(" = ?");
				comma = " AND ";
			}
		}
		if ((returning != null) && (returning.length > 0)) {
			sb.append(" RETURNING ");
			for (int I = 0; I < returning.length; I++) {
				if (I > 0) {
					sb.append(",");
				}
				sb.append(returning[I]).append(" AS ").append(tbl).append(returning[I]);
			}
		}
		_lastSQL = sb.toString();
		return _conn.getPreparedStatement(_lastSQL);
	}

	/**
	 * Builds a PreparedStatement for deleting records from the database
	 * @param tbl  Table to update in database
	 * @param pl1  Parameter List of constraint fields for where section
	 * @param returning  Fields to return from the update query
	 * @return Returns a PreparedStatement if successful otherwise null if an error occurs
	 */
	protected PreparedStatement buildDelete(String tbl, ParameterList pl, String[] returning) throws Exception {

		StringBuilder sb = (new StringBuilder("DELETE FROM "));
		sb.append(tbl).append(" WHERE ");
		StringBuilder sbWhere = new StringBuilder();
		String and = "";
		int paramCount = pl.getParamCount();
		for (int indx = 0; indx < paramCount; indx++) {
			sbWhere.append(and).append(pl.getParamName(indx)).append(" = ?");
			and = AND;
		}
		sb.append(sbWhere);
		if ((returning != null) && (returning.length > 0)) {
			sb.append(" RETURNING ");
			for (int I = 0; I < returning.length; I++) {
				if (I > 0) {
					sb.append(",");
				}
				sb.append(returning[I]);
			}
		}
		_lastSQL = sb.toString();
		PreparedStatement ps = _conn.getPreparedStatement(_lastSQL);
		pl.setPreparedStatement(ps);
		return ps;
	}

	/**
	 *  Returns the last set of returned records and then clears the _lastReturning object
	 * @return  Returns the JSONArray of last returned fields
	 */
	public JSONObject getLastReturning() {
		JSONObject lr = _lastReturning;
		clearLastReturning();
		return lr;
	}

	public void setLastReturning(JSONObject lastReturning) {
		this._lastReturning = lastReturning;
	}
	/**
	 * Clears the _lastReturning object
	 */
	public void clearLastReturning() {
		_lastReturning = null;
	}

	/**
	 * Helper Method to return all the fields for the readRecords Method
	 * @param emap  Table to Entity mapping object
	 * @param data  JsonObject containing the constraint data
	 * @return  Result record in JsonObject format
	 * @throws Exception 
	 */
	public JSONObject completeRecord(EntityMap emap, JSONObject data) throws Exception {
		return getRecordAsJSON(readRecords(emap, data), emap.getAllFields(), null);
	}

	/**
	 * Returns the first record in a ResultSet in JSONObject format
	 * @param rs  Record source for returning record
	 * @param flds  Fields to return from the resultset
	 * @param fieldPrefix  prefix to add the field names
	 * @return  JSONObject is returned if the call succeeds otherwise null if an error occurs
	 */
	public JSONObject getRecordAsJSON(ResultSet rs, String[] flds, String fieldPrefix) {
		JSONArray array = buildJSON( rs, flds, fieldPrefix);
		if (array.length() > 0) {
			try {
				return array.getJSONObject(0);
			} catch (Exception e) {
				setError(e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Converts the resultset into a JSONArray 
	 * @param rs  Source of records for the JSONArray
	 * @param flds  List of fields to include in the returned JSONObjects
	 * @param prefix  Optional Field prefix to add on if necessary
	 * @return  JSONArray if successful otherwise null if error occurs
	 */
	public JSONArray buildJSON(ResultSet rs, String[] flds, String prefix) {
		JSONArray array = new JSONArray();

		try {
			if (rs == null) {
				return array;
			}
			ArrayList<String> aFlds = getFieldIntersect(flds, rs.getMetaData()); 
			Object obj;
			JSONObject json;
			prefix = (prefix == null) ? "" : prefix;

			while (rs.next()) {
				json = new JSONObject();
				for (String fName : aFlds) {
					try {
						obj = rs.getObject(prefix + fName);
					} catch (Exception sqle) {
						GLogger.error("DBInteract.buildJSON Field not found(" + fName + ")");
						continue;
					}
					if (obj == null) {
						obj = "";
					} else if (obj instanceof java.sql.Date) {
						obj = DateUtil.DateFormat2.format(obj);
					}
					json.put(fName, obj);
				}
				array.put(json);
			}
		} catch (Exception e) {
			setError("buildJSON " + e.getMessage());
		}
		return array;
	}
	
	public static ArrayList<String> getFieldIntersect(String[] fields, ResultSetMetaData rsmd) throws SQLException {
		ArrayList<String> rtn = new ArrayList<String>();
		 int fLen = fields == null ? 0 : fields.length;
		 int colCount = rsmd.getColumnCount();
		 String colName;
		 
		 for (int indx = 0; indx < colCount; indx++) {
			 colName = rsmd.getColumnName(indx + 1); 
			 if (fLen > 0) {
				 for (String fname : fields) {
					 if (fname.trim().equalsIgnoreCase(colName)) {
						 rtn.add( colName ); 
					 }
				 }
			 } else {
				 rtn.add( colName ); 
			 }
		}
		return rtn;
	}

	public static JSONObject getReturning(ResultSet rs) throws SQLException, JSONException {
		JSONObject json = new JSONObject();

		if (rs == null) {
			return json;
		}
		
		ResultSetMetaData rsmd = rs.getMetaData();

		String fName;
		Object obj;

		if (rs.next()) {
			int colCount = rsmd.getColumnCount();
			for (int indx = 0; indx < colCount; indx++) {
				fName = rsmd.getColumnName(indx + 1);
				obj = rs.getObject(fName);
				if (obj == null) {
					obj = "";
				} else if (obj instanceof java.sql.Date) {
					obj = DateUtil.DateFormat2.format(obj);
				}
				json.put(fName, obj);
			}
		}
		return json;
	}

	/**
	 * Set whether or not the JsonWriter will encode all return string values
	 * @param encode  if true all strings will be Base64 encoded otherise they won't
	 */
	public void setEncode(boolean encode) {
		if (_jwriter != null) {
			_jwriter.setEncode(encode);
		}
	}

	/**
	 * Sets the error field and writes an error to the JsonWriter
	 * @param errMsg  Error string to write
	 */
	public boolean setError(String errMsg) {
		if (_jwriter != null) {
			_error = errMsg;
			_jwriter.writePair("error", _error);
		} else {
			GLogger.error(errMsg);
		}
		return false;
	}

	public boolean setError(Exception e) {
		if (_jwriter != null) {
			String error = e.getMessage().replaceAll("\"","\'");			
			setError(error);
		} else {
			GLogger.error(e);
		}
		return false;
	}

	public String getError() {
		String error = _error;
		_error = null;
		return error;
	}

	public boolean hasError() {
		return _error != null;
	}

	/**
	 * Clears out variables for garbage collection
	 */
	public void dispose() {
		DBConnMgr.release(_conn);
		if (_jwriter != null) {
			_jwriter.dispose();
		}
	}
	
	public void closeAll() {
		if (_conn != null) {
			_conn.closeAll();
		}
	}

}
