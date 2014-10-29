package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.db.ParameterList;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.util.DateUtil;
import org.geof.util.Expression;
import org.geof.util.FileUtil;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;

import com.adobe.xmp.impl.Base64;


/**
 * Derived DBRequest object for interacting with Search table.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class SearchRequest extends DBRequest {

	public final static String ENTITYNAME = "search";

	public final static String FILE = "file";
	public final static String FILE_KEYWORDS = "file_keyword";
	public final static String FILE_PROJECT = "file_project";
	public final static String KEYWORDS = "keywords";
	public final static String PROJECTS = "projects";
	public final static String KEYWORD = "keyword";
	public final static String PROJECT = "project";
	public final static String KEYWORDID = "keywordid";
	public final static String PROJECTID = "projectid";
	public final static String FILETYPES = "filetypes";

	public final static int BOUNDINGBOX = 0;
	public final static int AFTERBEFORE = 0;
	public final static int AFTER = 1;
	public final static int BEFORE = 2;

	public final static int GEOM_POINT = 0;
	public final static int GEOM_LINE = 1;

	public final static String SEARCHID = "searchid";
	protected static final String POINTSRID = "pointsrid";
	protected static final int DEFAULTSRID = 4326;

	protected static final String ST_INTERSECTS = "ST_Intersects(geom,?)";
	protected static final String ST_DISTANCE_SPHERE = "ST_Distance_Sphere(geom, ?) <= ?";
	protected static final String ST_DWITHIN = "ST_DWithin(transform(geom, 2163),transform(?,2163),?)";// "ST_DWithin(geom, ?, ?)"
	
	protected static final String SEARCH_COLUMNS = "duration,geomtype,filetype,viewid,storagelocid,notes,registeredby,registerdate,createdate,checksumval,status,originalname,filesize,fileext,filename,id";
	
	// ----------------------------------------------------------
	public SearchRequest() {
		_entityname = ENTITYNAME;
	}

	// ----------------------------------------------------------

	/**
	 * Overrides the base class process() to provide Keyword specific action
	 * handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(EXECUTE)) {
			return execute();

		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		} else {
			return super.process();
		}
	}

	@Override
	public boolean read() {
		try {
			int searchid = _where.optInt( ID, -1);
			if (searchid == -1) {
				return super.read();
			}
			JSONObject json = _dbi.completeRecord(_entitymap, _data);

			List<String> keywords = SearchRequest.getKeywords(_dbi, searchid);
			if (keywords.size() > 0) {
				JSONArray words = new JSONArray();
				for (String word : keywords) {
					words.put( Base64.encode(word) );
				}
				json.put(KEYWORDS, words);
			}

			List<Integer> pids = SearchRequest.getProjects(_dbi, searchid);
			if (pids.size() > 0) {
				JSONArray projectids = new JSONArray();
				for (Integer id : pids) {
					projectids.put( id );
				}
				json.put(PROJECTS, projectids);
			}
			_jwriter.openArray(DATA);
			_jwriter.writeValue(json);
			_jwriter.closeArray();
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}
	
	public static List<String> getKeywords(DBInteract dbi, int searchid) {
		return getKeywords( dbi, searchid, false);
	}
	
	public static List<String> getKeywords(DBInteract dbi, int searchid, boolean encode) {
		ArrayList<String> words = new ArrayList<String>();
		try {
			String[] cols = { KEYWORD };
			ResultSet rs = LinkRequest.readLinked(dbi, KEYWORD, ENTITYNAME, searchid, cols);
			String word;
			if (rs != null) {
				while (rs.next()) {
					word = rs.getString(1);
					if (encode) {
						word = Base64.encode(word);
					}
					words.add(word);
				}
			}
		} catch (SQLException e) {
			GLogger.error(e);
		}
		return words;
	}
	
	public static List<Integer> getProjects(DBInteract dbi, int searchid) {
		ArrayList<Integer> pids = new ArrayList<Integer>();
		try {
			String[] cols = { ID };
			ResultSet rs = LinkRequest.readLinked(dbi, PROJECT, ENTITYNAME, searchid, cols);
			if (rs != null) {
				while (rs.next()) {
					pids.add(rs.getInt(1));
				}
			}
		} catch (SQLException e) {
			GLogger.error(e);
		}
		return pids;
	}
	
	@Override
	public boolean update() {
		try {
			boolean rtn = super.update();
			if (rtn) {
				JSONObject where = new JSONObject();
				Long searchid = _where.getLong(ID);
				where.put(ID, searchid);
				String error = LinkRequest.deleteAllLinks(_dbi, ENTITYNAME, where);
				if (error != null) {
					return setError(error);
				}
				JSONObject data = new JSONObject();
				data.put(ENTITYNAME + ID, searchid);
				if (_data.has(PROJECTS)) {
					JSONArray projects = _data.getJSONArray(PROJECTS);
					for (int indx = 0; indx < projects.length(); indx++) {
						data.put(PROJECTID, projects.getLong(indx));
						error = LinkRequest.createLink(_dbi, ENTITYNAME, PROJECT, data);
						if (error != null) {
							return setError(error);
						}
					}
				}
				data.remove(PROJECTID);
				if (_data.has(KEYWORDS)) {
					JSONArray keywords = _data.getJSONArray(KEYWORDS);
					
					int[] kwIds = KeywordRequest.getIDs(keywords, _dbi, true);
					for (int indx = 0; indx < kwIds.length; indx++) {
						data.put(KEYWORDID, kwIds[indx]);
						error = LinkRequest.createLink(_dbi, ENTITYNAME, KEYWORD, data);
						if (error != null) {
							return setError(error);
						}
					}
				}
			}
			return rtn;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}

	/**
	 * This method executes the Search sent in using the _data JSONObject and
	 * writes the matching MediaObjects out the response stream as a list.
	 * 
	 * @return Returns true if the method succeeds otherwise false if an error
	 *         occurs
	 */

	public boolean execute() {
		boolean rtn = false;
		try {

			// GLogger.verbose("Running search");
			if (!_data.has(WHERE)) {
				setError("SearchRequest.execute needs a where section");
				return false;
			}
			JSONObject data = _data;
			if (_where.has(ID)) {
				data = this.getDataById();
				if (data == null) {
					return false;
				}
				if (data != null) {
					data.put(COLUMNS, SEARCH_COLUMNS);
				}
				data.put(FILETYPES, _data.optString(FILETYPES,FileUtil.FILETYPE_ENUM));
			}			
			return this.execute(data);
			
		} catch (Exception e) {
			setError(e);
		}
		return rtn;
	}
	
	public JSONObject getDataById() {
		JSONObject data = new JSONObject();
		try {
			int searchid = _where.optInt( ID, -1);
			if (searchid == -1) {
				setError("Where element mission id (searchid)");
				return null;
			}
			JSONArray aray = _dbi.readAsJson( _entitymap, _data);
			if (aray.length() != 1) {
				setError("Search record not found - id : " + searchid);
				return null;
			}
			JSONObject where = aray.getJSONObject(0);
			data.put(WHERE, where);
			List<Integer> pids = getProjects(_dbi, searchid);
			JSONArray jPids = new JSONArray();
			for (int pid : pids) {
				jPids.put(pid);
			}
			List<String> kwords = getKeywords(_dbi, searchid,true);
			JSONArray jKwords = new JSONArray();
			for (String word : kwords) {
				jKwords.put(word);
			}
			data.put(PROJECTS, jPids);
			data.put(KEYWORDS, jKwords);
			return data;
			
		} catch (Exception e) {
			setError(e);
			return null;
		}
/*
 "data":{
    "where":{
        "name":"copy test",
        "description":"copy test",
        "usespatial":true,
        "spatialtype":0,
        "distance":0,
        "unitofmeasure":0,
        "minlat":"40.56567",
        "minlon":" -105.12185",
        "maxlat":"40.61781",
        "maxlon":" -105.03259",
        "usetemporal":true,
        "temporaltype":"2",
        "mindatetime":null,
        "maxdatetime":"2013-04-19 00:00:00",
        "status":1,
        "usekeywords":true,
        "keywordany":false,
        "keywordexact":false,
        "useprojects":true,
        "keywordtype":"1"
    },
    "projects":[
        5,
        7
    ],
    "keywords":[
        "dGVzdA==",
        "Y29weQ=="
    ]
*/
		
	}

	public boolean execute(JSONObject data) {
		boolean rtn = false;
		try {

			String filetypes = data.optString(FILETYPES, "");
			if (filetypes.length() == 0) {
				filetypes = "0,1,2,3,4,5";
			}
			JSONObject where = JsonUtil.getJsonObject(data, WHERE, new JSONObject());
			SearchCriteria sc = new SearchCriteria();

			sc._minlat = where.optString( "minlat", "0.0");
			sc._minlon = where.optString( "minlon", "0.0");
			sc._maxlat = where.optString( "maxlat", "0.0");
			sc._maxlon = where.optString( "maxlon", "0.0");
			sc._mindatetime = where.optString( "mindatetime", null);
			sc._maxdatetime = where.optString( "maxdatetime", null);
			sc._distance = where.optString( "distance", "0.0");

			sc._unitofmeasure = where.optInt( "unitofmeasure", 0);

			sc._spatialtype = where.optInt( "spatialtype", 0);
			sc._temporaltype = where.optInt( "", 0);
			sc._usespatial = where.optBoolean( "usespatial", false);
			sc._usetemporal = where.optBoolean( "usetemporal", false);
			sc._usekeywords = where.optBoolean( "usekeywords", false);
			sc._useprojects = where.optBoolean( "useprojects", false);
			sc._keywordtype = where.optInt( "keywordtype", 0);

			int searchid = where.optInt( ID, -1);

			String sortfield = "id";
			String direction = "ASC";
			if (data.has(ORDERBY)) {
				JSONObject joOrderBy = data.getJSONObject(ORDERBY);
				direction = joOrderBy.optInt( DIRECTION, 1) > 0 ? "ASC" : "DESC";
				sortfield = joOrderBy.optString( SORTFIELD, "id");
			}

			sc._keywords = new ArrayList<String>();
			if (sc._usekeywords) {
				if (data.has(KEYWORDS)) {
					JSONArray jaKeywords = data.getJSONArray(KEYWORDS);
					int len = jaKeywords.length();
					for (int indx=0; indx < len; indx++) {
						sc._keywords.add(Base64.decode(jaKeywords.getString(indx)));
					}
				} else if (searchid > -1) {
					String[] cols = { KEYWORD };
					ResultSet rs = LinkRequest.readLinked(_dbi, KEYWORD, _entitymap.getName(),
							searchid, cols);
					if (rs != null) {
						while (rs.next()) {
							sc._keywords.add(rs.getString(1));
						}
					}
				}
			}

			sc._projectids = "";
			if (sc._useprojects) {
				if (data.has(PROJECTS)) {
					sc._projectids= data.getJSONArray(PROJECTS).join(",");

				} else if (searchid > -1) {
					String[] idcols = { ID };
					ResultSet rs = LinkRequest.readLinked(_dbi, PROJECT, _entitymap.getName(),
							searchid, idcols);
					if (rs != null) {
						while (rs.next()) {
							if (sc._projectids.length() > 0) {
								sc._projectids += ",";
							}
							sc._projectids += (rs.getInt(1));
						}
					}
				}
			}

			StringTokenizer stSortField = new StringTokenizer(sortfield, ",");
			EntityMap emap = EntityMapMgr.getEMap(FILE);
			sortfield = stSortField.hasMoreTokens() ? stSortField.nextToken() : sortfield;

			String[] columns = null;
			if (data.has(COLUMNS)) {
				columns = data.getString(COLUMNS).split(",");
			}
			_jwriter.writePair("searchid", searchid);
			_jwriter.openArray("data");

			if (sc._usespatial) {
				// run the query for points and lines
				for (int geomtype = 0; geomtype < 2; geomtype++) {
					PreparedStatement ps = buildStatement(sc, columns, sortfield, direction,filetypes, geomtype);
					ResultSet rs = ps.executeQuery();
					_jwriter.writeResultset(rs, emap.getDefaultFields());
					ps.close();
				}
			} else {
				PreparedStatement ps = buildStatement(sc, columns, sortfield, direction, filetypes,-1);
				ResultSet rs = ps.executeQuery();
				_jwriter.writeResultset(rs, emap.getDefaultFields());
				ps.close();
			}
			_jwriter.closeObject();
		} catch (Exception e) {
			setError(e.getMessage());
		}
		return rtn;
	}

	/**
	 * Method builds the complex statement needed to perform the spatial,
	 * temporal, and keyword search of MediaObjects.
	 * 
	 * @param emap
	 *            Contains the EntityMap for the MediaObject being queried
	 * @param fieldlist
	 *            List of fields to return from the EntityMap search table
	 * @return Returns a PreparedStatement which queries the EntityMap table.
	 */
	private PreparedStatement buildStatement(SearchCriteria sc, String[] columns, String sortfield,
			String direction, String filetypes, int geomtype) throws Exception {

		EntityMap fileEmap = EntityMapMgr.getEMap(FILE);
		ParameterList pl = new ParameterList(fileEmap);

		String sql = "SELECT DISTINCT ";

		if (columns != null) {
			for (int indx = 0; indx < columns.length; indx++) {
				if (indx > 0) {
					sql += ",";
				}
				sql += "f." + columns[indx].trim();
			}			
		} else {
			sql += "f.id,f.fileext,f.storagelocid,f.filesize,"
				+ "f.status,f.createdate,f.filename,f.filetype,f.geomtype,f.notes,f.originalname";
		}
		if (geomtype == 0) {
			sql += ",p.utcdate,p.latitude,p.longitude";				
		} else if (geomtype == 1) {
			sql += ",l.startdate,l.enddate,l.maxlon,l.maxlat,l.minlon,l.minlat";
		}

		sql += " FROM file f";	
		String where = " f.filetype IN (" + filetypes + ")";

		// TODO: add code to handle passed in comparators
		if (sc._usetemporal) {
			
			if ((sc._mindatetime != null) && (DateUtil.isValid(sc._mindatetime))) {
				where += " AND f.createdate" + Expression.GTEQ;
				pl.add("createdate", DateUtil.parseTimestamp(sc._mindatetime));
			}
			if ((sc._maxdatetime != null) && (DateUtil.isValid(sc._maxdatetime))){
				where += " AND f.createdate" + Expression.LTEQ;
				pl.add("createdate", DateUtil.parseTimestamp(sc._maxdatetime));
			}
		}

		if (sc._usespatial && ((geomtype == 0) || (geomtype == 1))) {
			if (geomtype == 0) { // check points
				sql += ",file_point fp, point p";
				where += " AND f.id = fp.fileid AND fp.pointid = p.id";

			} else { // check lines
				sql += ",file_line fl, linepoint p, line l";
				where += " AND f.id = fl.fileid AND fl.lineid = p.lineid AND fl.lineid = l.id";
			}

			if (sc._spatialtype == BOUNDINGBOX) {
				// where += " AND lp.latitude >= " + sc._minlat
				// + " AND lp.longitude >= " + sc._minlon
				// + " AND lp.latitude <= " + sc._maxlat
				// + " AND lp.longitude <= " + sc._maxlon;

				where += " AND ST_Intersects(p.geom,?)";
				pl.add("boundingbox", createPolygon(sc));
			} else {
				where += " AND ST_DWithin(transform(lp.geom, 2163),transform(?,2163),?)";
				pl.add("centerpoint", getPoint(sc._minlon, sc._minlat));
				pl.add("distance", Double.parseDouble(sc._distance));
			}
		}

		if ((sc._useprojects) && (sc._projectids != null) && (sc._projectids.length() > 0)) {
			sql += ",file_project fpr ";
			where += " AND f.id = fpr.fileid AND fpr.projectid IN (" + sc._projectids + ")";
		}

		if ((sc._usekeywords) && (sc._keywords != null) && (sc._keywords.size() > 0)) {
			where += " AND (";
			String keywordAny = (sc._keywordtype == 1) ? Expression.AND : Expression.OR;
			String keywordExact = (sc._keywordtype == 0) ? "" : "%";

			for (int kIndx = 0; kIndx < sc._keywords.size(); kIndx++) {
				if (kIndx > 0) {
					where += keywordAny;
				}
				pl.add(sc._keywords.get(kIndx), keywordExact + sc._keywords.get(kIndx)
						+ keywordExact);
				where += " f.id IN (SELECT DISTINCT fw.fileid FROM file_keyword fw, keyword k WHERE fw.keywordid = k.id AND k.keyword LIKE ?)";
			}
			where += " )";
		}

		sql += " WHERE" + where;

		if (sortfield != null) {
			sql += " ORDER BY " + sortfield + " " + direction;
		} else {
			sql += " ORDER BY createdate desc";
		}
//		GLogger.debug(sql.toString());
		PreparedStatement ps = _dbi.getPreparedStatement(sql);
		pl.setPreparedStatement(ps);
		return ps;
	}

	/**
	 * Writes the current Search criteria to the log file for debug purposes
	 */
	@SuppressWarnings("unused")
	private void writeToLog(SearchCriteria sc) {
		GLogger.verbose("_minlat " + sc._minlat);
		GLogger.verbose("_minlon " + sc._minlon);
		GLogger.verbose("_maxlat " + sc._maxlat);
		GLogger.verbose("_maxlon " + sc._maxlon);
		GLogger.verbose("_mindatetime " + sc._mindatetime);
		GLogger.verbose("_maxdatetime " + sc._maxdatetime);
		GLogger.verbose("_distance " + sc._distance);

		GLogger.verbose("_unitofmeasure " + sc._unitofmeasure);
		GLogger.verbose("_spatialtype " + sc._spatialtype);
		GLogger.verbose("_temporaltype " + sc._temporaltype);

		GLogger.verbose("_usespatial " + sc._usespatial);
		GLogger.verbose("_usetemporal " + sc._usetemporal);
		GLogger.verbose("_useprojects " + sc._useprojects);

		GLogger.verbose("_usekeywords " + sc._usekeywords);
		GLogger.verbose("_keywords " + sc._keywords);
		GLogger.verbose("_keywordtype " + sc._keywordtype);
	}

	/**
	 * Converts passed in parameters into a JSONObject version of a Keyword
	 * Object
	 * 
	 * @param keyword
	 *            Keyword string
	 * @param confidence
	 *            Keyword confidence
	 * @return Returns a JSONObject version of a Keyword Object
	 */
	protected JSONObject getKeywordIDAsJSON(String keyword, double confidence) {
		try {
			long keywordid = KeywordRequest.getID(keyword, _dbi, true);
			if (keywordid == -1) {
				return null;
			}
			JSONObject key = new JSONObject();
			key.put("id", keywordid);
			key.put("confidence", confidence);
			return key;
		} catch (Exception e) {
			setError(e);
			return null;
		}
	}

	/**
	 * 
	 * @return Returns a Polygon object created using the Search's min and max
	 *         latitude and longitude values for use as a bounding box.
	 */
	protected Polygon createPolygon(SearchCriteria sc) {
		double minlon = Double.parseDouble(sc._minlon);
		double minlat = Double.parseDouble(sc._minlat);
		double maxlon = Double.parseDouble(sc._maxlon);
		double maxlat = Double.parseDouble(sc._maxlat);
		Point[] points = { new Point(minlon, minlat), new Point(maxlon, minlat),
				new Point(maxlon, maxlat), new Point(minlon, maxlat), new Point(minlon, minlat) };
		return createPolygon(points);
	}

	/**
	 * 
	 * @return Returns a Polygon object created using an array of point object
	 *         values for use as a bounding box.
	 */
	protected Polygon createPolygon(Point[] points) {
		LinearRing lr = new LinearRing(points);
		lr.setSrid(GlobalProp.instance().getInt(POINTSRID, DEFAULTSRID));
		Polygon polygon = new Polygon((new LinearRing[] { lr }));
		polygon.setSrid(lr.getSrid());
		return polygon;
	}

	/**
	 * Converts string value latitude and longitude into a Point object
	 * 
	 * @param longitude
	 *            Longitude value in string form
	 * @param latitude
	 *            Latitude value in string form
	 * @return
	 */
	protected Point getPoint(String longitude, String latitude) {
		Point point = new Point(Double.parseDouble(longitude), Double.parseDouble(latitude));
		point.setSrid(GlobalProp.instance().getInt(POINTSRID, DEFAULTSRID));
		return point;
	}

	private class SearchCriteria {
		public String _minlat;
		public String _minlon;
		public String _maxlat;
		public String _maxlon;
		public String _mindatetime;
		public String _maxdatetime;
		public String _distance;

		public int _unitofmeasure;
		public int _spatialtype;
		public int _temporaltype;

		public boolean _usespatial;
		public boolean _usetemporal;
		public boolean _useprojects;
		public String _projectids;

		public boolean _usekeywords;
		public ArrayList<String> _keywords;
		public int _keywordtype;

	}
}
