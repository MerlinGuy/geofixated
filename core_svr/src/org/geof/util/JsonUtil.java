package org.geof.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.geof.log.GLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JsonUtil class provides helper methods for working with JSONObjects and JSONArrays
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class JsonUtil {

	public final static String WHERE = "where";
	public final static String ID = "id";
	public final static String FIELDS = "fields";
	public final static String DATA = "data";
	public final static String ENTITY = "entity";
	public final static String ACTION = "action";
	public final static String COLUMNS = "columns";
	public final static String ORDERBY = "orderby";
	
	public static boolean equal(JSONObject json, String field, String value) {
		if ( value != null && json != null && json.has(field)) {
			return value.equalsIgnoreCase(json.optString(field));
		}
		return false;
	}

	public static Map<String, Object>  getMap(JSONObject flds) {
		Map<String,Object> map = new HashMap<String,Object>();
		Iterator<?> keys = flds.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			map.put(key, flds.optString(key));
		}
		return map;
	}
	
	/**
	 * Method returns the value of a specified field as a Date value if it exists otherwise it
	 * returns the default value.
	 * 
	 * @param json JSONObject to retrieve the value from.
	 * @param name Name of the field to retrieve the Date value from.
	 * @param dftvalue Default Date value to return if the named field does not exist in the
	 * JSONObject
	 * @return Returns the value of a specified field as a Date value if it exists otherwise
	 * it returns the default value.
	 */
	public static Date getDate(JSONObject json, String name, Date dftvalue) {
		if (json == null || !json.has(name))
			return dftvalue;
		try {
			return DateUtil.DateFormat2.parse(json.getString(name));
		} catch (Exception e) {
			return dftvalue;
		}
	}

	/**
	 * Method returns the value of a specified field as a JSONObject value if it exists
	 * otherwise it returns the default value.
	 * 
	 * @param json JSONObject to retrieve the value from.
	 * @param name Name of the field to retrieve the JSONObject value from.
	 * @param dftvalue Default JSONObject value to return if the named field does not exist in
	 * the JSONObject
	 * @return Returns the value of a specified field as a JSONObject value if it exists
	 * otherwise it returns the default value.
	 */
	public static JSONObject getJsonObject(JSONObject json, String name, JSONObject dftvalue) {
		if (json == null) {
			return dftvalue;
		} else {
			JSONObject jo = json.optJSONObject(name);			
			return jo == null ?  dftvalue : jo;			
		}
	}

	public static JSONObject getJsonPath(JSONObject json, String[] searchTree) {
		JSONObject rtn = json;
		try {
			for (String key : searchTree) {
				rtn = rtn.getJSONObject(key);
			}
			return rtn;
		} catch (JSONException e) {
			GLogger.error("JsonUtil.getJsonPath : "  + e.getMessage());
			return null;
		}
	}

	public static JSONArray getJsonArray(JSONObject json, String name, JSONArray dftvalue) {
		if (json != null && json.has(name)) {
			return json.optJSONArray(name);
		} else {
			return dftvalue;
		}
	}
	
  public static JSONObject getData(Object[] fields, Object[] where) {
  try {
      JSONObject data = new JSONObject();
      if (fields != null && ((fields.length % 2) == 0)) {
          JSONObject jfields = new JSONObject();
          int indx = 0;
          int count = fields.length;
          while (indx < count) {
              jfields.put((String) fields[indx], fields[indx+1]);
              indx += 2;
          }
          data.put(FIELDS, jfields);
      }
      if ((where != null) && ((where.length % 2) == 0)) {
          JSONObject jwhere = new JSONObject();
          int indx = 0;
          int count = where.length;
          while (indx < count) {
              jwhere.put((String) where[indx], where[indx+1]);
              indx += 2;
          }
          data.put(WHERE, jwhere);
      }
      return data;
  } catch (Exception e) {
      GLogger.error(e);
      return null;
  }
}
	
	/**
	 * Builds a standard JSONObject with field and value added to the Where section
	 * 
	 * @param fieldname Field name of value
	 * @param value Value to add
	 * @return JSONObject in standard Data format
	 */
	public static JSONObject getDataWhere(String fieldname, Object value) {
		try {
			JSONObject data = new JSONObject();
			JSONObject where = new JSONObject();
			if (fieldname != null) {
				where.put(fieldname, value);
			}
			data.put("where", where);
			return data;
		} catch (Exception e) {
			GLogger.error(e);
			return null;
		}
	}

	/**
	 * Builds a standard JSONObject with an imbedded WHERE object which included the ID field
	 * and id value
	 * 
	 * @param id Value of the ID field to add
	 * @return Returns a JSOBObject in the standard Data format.
	 */
	public static JSONObject getDataWhere(long id) {
		return getDataWhere(ID, id);
	}

	/**
	 * Builds a standard JSONObject with an imbedded WHERE object 
	 * 
	 * @param where JSONObject placed in the data JSONObject
	 * @return Returns a JSOBObject in the standard Data format.
	 */
	public static JSONObject getDataWhere(JSONObject where) {
		try {
			JSONObject data = new JSONObject();
			data.put(WHERE, where);
			return data;
		} catch (Exception e) {
			GLogger.error(e);
			return null;
		}
	}

	public static JSONObject getWhere(String fieldname, Object value) {
		try {
			JSONObject where = new JSONObject();
			if (fieldname != null) {
				where.put(fieldname, value);
			}
			return where;
		} catch (Exception e) {
			GLogger.error(e);
			return null;
		}
	}
	
	public static JSONObject getFields(String fieldname, Object value) {
		try {
			JSONObject fields = new JSONObject();
			if (fieldname != null) {
				fields.put(fieldname, value);
			}
			return fields;
		} catch (Exception e) {
			GLogger.error(e);
			return null;
		}
	}
	/**
	 * Creates a new JSONObject in Data format with a FIELD JSONObject included.
	 * 
	 * @return Returns the newly created JSONObject
	 */
	public static JSONObject getDataFields() throws JSONException{
		return getDataFields(new JSONObject());
	}

	/**
	 * Creates a new JSONObject in Data format with a FIELD JSONObject included.
	 * 
	 * @return Returns the newly created JSONObject
	 */
	public static JSONObject getDataFields(JSONObject fields) throws JSONException{
			JSONObject data = new JSONObject();
			data.put(FIELDS, fields);
			return data;
	}
	
	public static JSONObject getFieldsWhere (String fieldsKey, Object fieldsValue, String whereKey, Object whereValue) {
		
		JSONObject data = new JSONObject();
		try {
			JSONObject fields = new JSONObject();
			JSONObject where = new JSONObject();
			fields.put(fieldsKey, fieldsValue);
			where.put(whereKey, whereValue);
			data.put(FIELDS,fields);
			data.put(WHERE, where);
		} catch (Exception e){}
		
		return data;
	}
	
	public static JSONObject getDataRead(String columns, JSONObject where, String orderby) throws JSONException {
		JSONObject data = new JSONObject();
		if (columns != null) {
			data.put(COLUMNS, columns);
		}
		if (where != null) {
			data.put(WHERE, where);
		}
		if (orderby != null) {
			data.put(ORDERBY, columns);
		}
		return data;
	}

	/**
	 * Creates a new JSONObject in Data format with a WHERE subobject included and the ID
	 * WHERE field set to the specified value
	 * 
	 * @param id Value of the ID field to add
	 * @return Returns the newly created JSONObject
	 */
	public static JSONObject getDataFields(long id) throws JSONException{
		JSONObject data = new JSONObject();
		JSONObject where = new JSONObject();
		where.put(ID, id);
		data.put(WHERE, where);
		data.put(FIELDS, new JSONObject());
		return data;
	}
	
	public static JSONObject copyTo(JSONObject src, JSONObject tar, boolean override) {
		try {
			if (tar == null) {
				tar = new JSONObject();
			}
			if (src != null) {
				Iterator<?> keys = src.keys();
				while (keys.hasNext()) {
					String key = (String)keys.next();
					if ( ! tar.has(key) || override ) {
						tar.put(key, src.get(key) );
					}
				}
			}
		} catch (Exception e){
			GLogger.error(e);
		}
		return tar;
	}
		
	public static JSONObject copyFrom(JSONObject src, JSONObject tar, String keylist) {
		try {
			if (tar == null) {
				tar = new JSONObject();
			}
			if (src != null) {
				for (String key : keylist.split(",")) {
					if (src.has(key)) {
						tar.put(key, src.get(key));
					}
				}
			}
		} catch (Exception e){
			GLogger.error(e);
		}
		return tar;
	}
		
	public static JSONObject move(JSONObject src, JSONObject tar, String key, Object value) {
		try {
			if ( ! tar.has(key)) {
				tar.put(key, (src.has(key) ? src.get(key) : value) );
			}
		} catch (Exception e){
			GLogger.error(e);
		}
		return tar;
	}
	
	public static void remove(JSONObject src, String subObject, String elements) throws Exception {
		if (src == null) {
			return;
		}
		Object obj = src;
		if (subObject != null && src.has(subObject)) {
			obj = src.get(subObject); 
		}
		String[] fields = elements.split(",");

		if (obj instanceof JSONObject) {
			JSONObject jo = (JSONObject)obj;
			for ( String field : fields) {
				jo.remove(field);
			}
		} else if (obj instanceof String) {
			String c = "";
			for (String col : ((String)obj).split(",") ) {
				for (String field : fields) {
					if (field.equalsIgnoreCase(col)) {
						col = null;
						break;
					}
				}
				if (col != null) {
					c += "," + col;
				}
			}

			src.put(subObject, (c.length() > 0) ? c.substring(1) : c);
		}
	}
	
	public static void decode(JSONObject src, JSONArray encoded) {
		try {
			if (src != null) {
				String key = null;
				for (int eindx = 0; eindx < encoded.length(); eindx++) {
					key = encoded.getString(eindx);
					src.put(key, Base64.decodeBase64(src.optString(key).getBytes()));
				}
			}
			
		} catch (Exception e){
			GLogger.error(e);
		}
	}
		
	public static void encode(JSONObject src, JSONArray encoded) {
		try {
			if (src != null) {
				String key = null;
				for (int eindx = 0; eindx < encoded.length(); eindx++) {
					key = encoded.getString(eindx);
					src.put(key, Base64.encodeBase64(src.optString(key).getBytes()));
				}
			}
			
		} catch (Exception e){
			GLogger.error(e);
		}
	}
		
	/**
	 * Reads the tasker.conf file and build a JSONObject containing each task
	 * manager to create and it's config.
	 * 
	 * @param fileName
	 *            The full path to the tasker.conf file to read
	 * @return Returns the JSONObject of all task managers.
	 */
	public static JSONObject parseConfigFile(String filePath) {

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(filePath));
			FileChannel fc = stream.getChannel();
			long size = fc.size();
			FileChannel.MapMode mode = FileChannel.MapMode.READ_ONLY;
			MappedByteBuffer bb = fc.map(mode, 0, size);
			/* Instead of using default, pass in a decoder. */
			String jsonString = Charset.defaultCharset().decode(bb).toString();
			return new JSONObject(jsonString);
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
		return null;
	}

	public static boolean check(JSONObject json, String key, String errMsg, Object... errArgs) {
		if (json.has(key)) {
			return true;
		}
		GLogger.error(String.format(errMsg, errArgs));
		return false;
	}
	
	public static JSONArray getArray(String valuelist, String delimiter) {
		String delim = delimiter == null ? "," : delimiter;
		JSONArray array = new JSONArray();
		for (String value : valuelist.split(delim)) {
			array.put(value);
		}
		return array;
	}
	
	public static String getArrayAsCsv(String[] fields, String tbl) {
		StringBuilder sb = new StringBuilder();
		if (fields != null && fields.length > 0 ) {
			String prefix = tbl == null ? "," : "," + tbl + ".";
			for (String field : fields) {
				sb.append(prefix).append(field);
			}
		}
		return sb.length() > 0 ? sb.substring(1) : sb.toString();
	}
	
	public static String[] getArrayAsList(JSONArray array) {
		int count = array.length();
		String[] rtn = new String[count];
		for (int indx = 0; indx < count; indx++) {
			rtn[indx] = array.optString(indx);
		}
		return rtn;
	}
	
	public static JSONArray filter(JSONArray data, JSONObject where) throws Exception {
		String[] keys = JSONObject.getNames(where);
		if (keys == null || keys.length == 0) {
			return data;
		}
		
		JSONArray rtn = new JSONArray();
		JSONObject item;
		
		int length = data.length();
		for (int indx=0;indx<length;indx++) {
			item = data.getJSONObject(indx);
	        for ( String key : keys ){
	            if ( item.has(key)) {
	            	if (item.get(key).equals(where.get(key))) {
	            		rtn.put(item);
	            	}
	            }
	        }
		}		
		return rtn;
	}

    @SuppressWarnings("unchecked")
	public static <T> List<T> toList(JSONArray obj) {
    	if (obj == null) {
    		return null;
    	}
    	List<T> list = new ArrayList<T>();
    	for (int i=0; i < obj.length();i++) {
    		list.add((T) obj.opt(i));
    	}
    	return list;
    }
    
    public static JSONArray toJSONArray(ResultSet rs) throws Exception {
		JSONArray records = new JSONArray();
		if (rs != null) {
			String[] cols = null;

			JSONObject record;
			while (rs.next()) {
				if (cols == null) {
					cols = getColumns(rs);
				}
				record = new JSONObject();
				for (String column : cols) {
					record.put(column,rs.getObject(column.trim())); 
				}
				records.put(record);
			}
		}
		return records;
    }
    
	public static String[] getColumns(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int colCount = md.getColumnCount();
		String[] cols = new String[colCount];
		for (int indx = 0; indx < colCount; indx++) {
			cols[indx] = md.getColumnName(indx+1);
		}
		return cols;		
	}


	public static String replace(String src, JSONArray replacements, String search, String replace) throws Exception {
		if (replacements != null) {
	        JSONObject chg;
	        for (int indx=0; indx < replacements.length();indx++) {
	        	chg = replacements.getJSONObject(indx);
	        	src = src.replaceAll(chg.getString("search"), chg.getString("replace"));
	        }
		}
		return src;        
	}
	
    public static JSONArray getJArray(String path, JSONObject values, String[] targets) throws FileNotFoundException, JSONException{
    	String str = FileUtil.parseFile(path);
    	if (values != null) {
    		for (String search : targets) {
    			str = str.replace("%" + search, values.getString(search));
    		}
    	}
    	return new JSONArray(str);
    }
    
    public static JSONObject getJObject(String path, JSONObject values, String[] targets) throws JSONException, FileNotFoundException{
    	String str = FileUtil.parseFile(path);
    	if (values != null) {
    		for (String search : targets) {
    			str = str.replace("%" + search, values.getString(search));
    		}
    	}
    	str = str.replace("\n", "").replace("\r", "");
    	return new JSONObject(str);
    }

    public static <T> JSONArray sort(JSONArray jray, Comparator<T> comparator) {    	
    	List<T> list = toList(jray);
    	Collections.sort(list, comparator);
    	return new JSONArray(list);
    }
}
