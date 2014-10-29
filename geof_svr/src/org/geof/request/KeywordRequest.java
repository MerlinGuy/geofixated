package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import org.geof.db.DBInteract;
import org.geof.log.GLogger;
import org.geof.util.ConvertUtil;

/**
 * Derived DBRequest class to handle database interactions for the Keyword table
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class KeywordRequest extends DBRequest {

	
	public final static String ENTITYNAME = "keyword";
	
	public final static String CONFIDENCE = "confidence";
	public final static String STATUS = "status";
	public final static String CREATEDBY = "createdby";
	public final static String DESCRIPTION = "description";
	public static final String KEYWORDS = "keywords";

	public final static String _sqlCreate = "INSERT INTO keyword (keyword,status,createdby,description) VALUES (?,?,?,?) RETURNING id";
	public final static String _sqlUpdate = "UPDATE keyword SET status=?,description=? WHERE id = ?";
	public final static String _sqlDelete = "DELETE FROM keyword WHERE id = ?";
	public final static String _sqlCheck = "SELECT id FROM keyword WHERE keyword = ?";

	/**
	 * Class constructor
	 */
	public KeywordRequest() {
		_entityname = "keyword";
	}

	// ------------------------------------------------------

	/**
	 * Overrides the base class process() to provide Keyword specific action handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		} else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();

		} else {
			return super.process();
		}
	}

	/**
	 * Reads the requested keywords from the Keyword table in the database
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean read() {
		_dbi.setEncode(true);
		try {
			_dbi.read(_entitymap, _data);
			return true;
		} catch (Exception e) {
			return setError(e);
		} finally {
			_dbi.setEncode(false);
		}
	}

	/**
	 * Creates a new keyword in the Keyword table in the database and returns its primary key.
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean create() {
		try {
			JSONObject fields = _data.getJSONObject(FIELDS);
			String keyword = fields.getString(ENTITYNAME);
			String desc = fields.optString( DESCRIPTION, null);
			int status = fields.optInt( STATUS, 1);
			int keywordid = getID(keyword, desc, status, true, true, _dbi);
			if (_data.has(LINK)) {
				JSONObject link = _data.getJSONObject(LINK);
				String entityB = link.getString(ENTITY);
				int entityBid = link.getInt(ID);
				JSONObject linkData = new JSONObject();
				linkData.put("keywordid", keywordid);
				linkData.put(entityB + "id", entityBid);
				LinkRequest.createLink(_dbi, _entityname, entityB, linkData);
			}
			_jwriter.openArray("data");
			_jwriter.openObject();
			_jwriter.writePair("keywordid", keywordid);
			_jwriter.closeObject();
			_jwriter.closeArray();
			return (keywordid > -1);
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	/**
	 * Updates the status and description of a keyword in the database. It does not change the
	 * spelling of the keywords since this changes the actual word.
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean update() {
		PreparedStatement ps = null;
		try {
			if (!_data.has(ID)) {
				setError("Keyword update field ID not found");
				return false;
			}
			long keywordID = _data.getLong(ID);
			int status = _data.optInt( STATUS, 1);
			String description =_data.optString( DESCRIPTION, "");

			ps = _dbi.getPreparedStatement(_sqlUpdate);
			int indx = 1;
			ps.setInt(indx++, status);
			ps.setString(indx++, description);
			ps.setLong(indx, keywordID);
			ps.execute();
			return true;

		} catch (Exception e) {
			setError(e);
			return false;
		} finally {
			if (ps != null) {
				try { ps.close();} catch (Exception dbe){}
			}		}
	}

	/**
	 * Deletes a keyword from the database after it removes all links to that word.
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean delete() {
		PreparedStatement ps = null;
		try {
			if (!_data.has(ID)) {
				setError("Keyword update field ID not found");
				return false;
			}
			int id = _data.getInt(ID);
			
			// remove all links to the keyword
			JSONObject where = new JSONObject();
			where.put(_entityname + "id", id);
			LinkRequest.deleteAllLinks(_dbi, _entityname, where);

			ps = _dbi.getPreparedStatement(_sqlDelete);
			ps.setLong(1, id);
			ps.execute();
			return true;

		} catch (Exception e) {
			setError(e);
			return false;
		} finally {
			if (ps != null) {
				try { ps.close();} catch (Exception dbe){}
			}		}
	}

	/**
	 * Helper method for getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * @see getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * Retrieves the id for a given Keyword. If the Keyword does not exist it is created.
	 * 
	 * @param joKeyword JSONObject containing Keyword information
	 * @param conn Database connection to use
	 * @return True if no error occurs
	 */
	public static int getID(JSONObject joKeyword, DBInteract dbi) throws Exception {
		if (!joKeyword.has(ENTITYNAME))
			return -1;

		String keyword = joKeyword.getString(ENTITYNAME);
		String description = joKeyword.optString( DESCRIPTION, "");
		int status = joKeyword.optInt( STATUS, 1);
		boolean create = !joKeyword.has(ID);
		return getID(keyword, description, status, create, false, dbi);
	}

	/**
	 * Helper method for getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * @see getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * Retrieves the id for a given Keyword but assumes the keyword is Base64 encoded. If the
	 * Keyword does not exist it is created.
	 * 
	 * @param keyword Keyword text
	 * @param conn Database connection to use
	 * @return True if no error occurs
	 */
	public static int getIdDecoded(String keyword,DBInteract dbi) throws Exception {
		return getID(keyword, "", 1, true, true, dbi);
	}

	/**
	 * Helper method for getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * @see getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * Retrieves the id for a given Keyword but assumes the keyword is Base64 encoded. If the
	 * Keyword does not exist it is created.
	 * 
	 * @param joKeyword JSONObject containing keyword.
	 * @param conn Database connection to use
	 * @return True if no error occurs
	 */
	public static int getIdDecoded(JSONObject joKeyword, DBInteract dbi) throws Exception {
		String keyword = joKeyword.getJSONObject(DATA).getJSONObject(FIELDS).getString("keyword");
		return getID(keyword, "", 1, true, true, dbi);
	}

	/**
	 * Helper method for getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * @see getID(String, String, int, boolean, boolean, DBConnection)
	 * 
	 * Retrieves the id for a given Keyword but since the keyword is in the form of a string
	 * it assumes that it is no Base64 encoded. If the Keyword does not exist it is created.
	 * 
	 * @param keyword Keyword text
	 * @param conn Database connection to use
	 * @return True if no error occurs
	 */
	public static int getID(String keyword, DBInteract dbi, boolean encoded) throws Exception {
		return getID(keyword, "", 1, true, encoded, dbi);
	}

	public static int[] getIDs(JSONArray keywords, DBInteract dbi, boolean encoded) throws Exception {
		int length = keywords.length();
		int[] ids = new int[length];
		for ( int indx = 0; indx < length; indx++) {
			ids[indx] = getID(keywords.getString(indx), dbi, encoded);
		}
		return ids;
	}
	
	public static int[] getIDs(String[] keywords,DBInteract dbi, boolean encoded) throws Exception {
		int length = keywords.length;
		int[] ids = new int[length];
		for ( int indx = 0; indx < length; indx++) {
			ids[indx] = getID(keywords[indx], dbi, encoded);
		}
		return ids;
	}
	/**
	 * Retrieves the id for a given Keyword. If the Keyword does not exist it is created.
	 * 
	 * @param keyword String containing the search for or new keyword
	 * @param desc Description of the new keyword if created
	 * @param status Status of the new keyword if created
	 * @param create If true and keyword is not found in database then a new keyword will be
	 * created.
	 * @param decode If true the keyword is assumed to be Base64 encoded and will be decoded
	 * before created.
	 * @param conn DBConnection to use to access the database
	 * @return Returns the keyword's primary key if found or created otherwise it will return
	 * -1
	 */
	public static int getID(String keyword, String desc, int status, boolean create, boolean decode, DBInteract dbi) throws Exception {
		if (keyword == null) {
			return -1;
		}
		PreparedStatement ps = null;
		ResultSet rs = null;
		int keywordid = -1;
		try {
			try { // if decode fails assume the keyword is not encoded.
				if (decode) {
					keyword = ConvertUtil.decode(keyword);
				}
			} catch (Exception de) {
				
			}
			keyword = keyword.replace("\\", "\\\\").replace("'", "''").trim().toLowerCase();
			keywordid = queryID(keyword, dbi);

			if ((keywordid > -1) || (!create)) {
				
				return keywordid;
			}
			if (keywordid == -1) {
				ps = dbi.getPreparedStatement(_sqlCreate);
				int indx = 1;
				ps.setString(indx++, keyword);
				ps.setInt(indx++, status);
				ps.setLong(indx++, dbi.getUsrId());
				ps.setString(indx, desc);
				rs = ps.executeQuery();
				if ((rs != null) && rs.next()) {
					keywordid = rs.getInt(ID);
				}
			}
			return keywordid;

		} catch (Exception e) {
			throw e;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}

	}

	/**
	 * Decodes a new or searched for keyword
	 * @param codedKeyword  Keyword to be decoded
	 * @return  Returns the decoded Keyword
	 */
	public static String Decode(String codedKeyword) {
		try {
			String keyword = ConvertUtil.decode(codedKeyword);
			keyword = keyword.replace("\\", "\\\\");
			keyword = keyword.replace("'", "''");
			return keyword.toLowerCase();

		} catch (Exception e) {
			return codedKeyword;
		}
	}

	/**
	 * Queries the database for the passed in keyword and returns its ID if found.
	 * @param keyword  Keyword to search for.
	 * @param conn  DBConnection to use to access the database
	 * @return  Returns the keyword's ID if found in the database otherwise returns -1
	 */
	public static int queryID(String keyword, DBInteract dbi) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		int id = -1;
		try {
			ps = dbi.getPreparedStatement(_sqlCheck);
			ps.setString(1, keyword);
			rs = ps.executeQuery();
			if ((rs != null) && (rs.next())) {
				id = rs.getInt(ID);
			}
		} catch (Exception e) {
			GLogger.error(e);
		} finally {
			if (ps != null) {
				try {ps.close();} catch(Exception e) {}
			}
		}
		return id;
	}

	/**
	 * Retrieves the IDs for a list of keywords.
	 * @param jaKeywords  JSONArray of keywords.
	 * @param conn  DBConnection to use to access the database
	 * @return Returns an ArrayList of Keyword IDs
	 */
	public static ArrayList<Integer> getKeywordIDs(JSONArray jaKeywords, DBInteract dbi) {
		ArrayList<Integer> keywordids = new ArrayList<Integer>();
		int count = jaKeywords.length();
		int keywordid;
		String keyword;
		try {
			for (int indx = 0; indx < count; indx++) {
				keyword = jaKeywords.getString(indx);
				keywordid = getID(keyword, null, 0, false, true, dbi);
				if (keywordid > -1) {
					keywordids.add(keywordid);
				}
			}
		} catch (Exception e) {
			GLogger.error(e);
		}
		return keywordids;
	}
}
