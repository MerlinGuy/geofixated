package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.db.ParameterList;
import org.geof.log.GLogger;
import org.json.JSONException;
import org.json.JSONObject;

public class LinkRequest extends DBRequest {


	public final static String ENTITY_A = "entitya";
	public final static String ENTITY_B = "entityb";
	
	/**
	 * Overrides the base class process() to provide Link specific action handlers.
	 * 
	 * @return True if action was processed
	 */
	@Override
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(READ + ".link")) {
			return readLinked();
			
		}  else if (_actionKey.equalsIgnoreCase(CREATE)) {
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
	 * Creates link(s) between two entitys (entityA and entityB) where there exists a third
	 * entity named entityA_entityB or entityB_entityA in the database
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean create() {
		/*
		 * data = {
		 * 		'entitya':'<entitya_name>',
		 * 		'entityb':'<entityb_name>',
		 * 		'<entitya_name>id':<id>,
		 * 		'<entityb_name>id':<id>,
		 * 		'where':{  --- optional ---
		 * 			'<entitya_name>id':<id>,
		 * 		}
		 * }
		 */
		boolean rtn = false;
		try {
			
			String entityA =_data.optString( ENTITY_A, null);
			if ((entityA == null) || (entityA.length() == 0)) {
				setError("Link.create missing " + entityA);
				return false;
			}
			String entityB =_data.optString( ENTITY_B, null);
			if ((entityB == null) || (entityB.length() == 0)) {
				setError("Link.create missing " + entityB);
				return false;
			}
			
			// If _data contains a where object a deleteLinks 
			// will be executed first, all errors here are ignored.
			if (_data.has(WHERE)) {
				LinkRequest.deleteLinks(_dbi, entityA, entityB, _data.getJSONObject(WHERE)); 
			}
			
			String error = LinkRequest.createLink(_dbi, entityA, entityB, _data);
			if (error != null) {
				setError(error);
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			GLogger.error(e);
		}
		return rtn;
	}
	
	public static String createLink(DBInteract dbi, String entityA, String entityB, JSONObject data) {
		try {
			EntityMap emap = EntityMapMgr.getLinkMap(entityA, entityB);
			if (emap == null) {
				return "Link.createLink invalid link entities " + entityA + ", " + entityB;
			}
			String entityAidField = entityA + "id";
			int entityAid = data.optInt( entityAidField, -1);
			if (entityAid == -1) {
				return "Link.create missing " + entityAidField;
			}
			String entityBidField = entityB + "id";
			int entityBid = data.optInt( entityBidField, -1);
			if (entityBid == -1) {
				return "Link.create missing " + entityBidField;
			}
			
			String sql = "SELECT count(*) as cnt FROM " + emap.getName() 
					+ " WHERE " + entityAidField + " = ?"
					+ " AND " + entityBidField + " = ?";
			
			PreparedStatement ps = dbi.getPreparedStatement(sql);
			ps.setInt(1, entityAid);
			ps.setInt(2, entityBid);
			ResultSet rs = ps.executeQuery();
			if ((rs != null) && rs.next()) {
				if (rs.getInt(1) > 0) {
					return null;
				}
			}
			ps.close();
			
			sql = "INSERT INTO " + emap.getName() 
					+ "(" + entityAidField + ","
					+ entityBidField + ") VALUES (?,?)";
			
			ParameterList pl = new ParameterList(emap);
			pl.add(entityAidField, entityAid);
			pl.add(entityBidField, entityBid);
			
			ps = dbi.getPreparedStatement(sql);
	//		GLogger.debug("sql: " + sql);
			pl.setPreparedStatement(ps);
			ps.execute(); 
			return null;
		} catch (Exception e) {
			GLogger.error(e);
			return e.getMessage();
		}
	}

	/**
	 * Reads the links for two entitys (entityA and entityB) where there exists a third
	 * entity named entityA_entityB or entityB_entityA in the database
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean read() {
		String entityA =_data.optString( ENTITY_A, null);
		if ((entityA == null) || (entityA.length() == 0)) {
			return setError("Link.read missing " + entityA);
		}
		String entityB =_data.optString( ENTITY_B, null);
		if ((entityB == null) || (entityB.length() == 0)) {
			return setError("Link.read missing " + entityB);
		}
		EntityMap emap = EntityMapMgr.getLinkMap(entityA, entityB);
		if (emap == null) {
			return setError("Link.read invalid link entitys " + entityA + ", " + entityB);
		}
		String sql = "SELECT * FROM " + emap.getName();
		try {
			ResultSet rs = _dbi.getResultSet(sql, null);
			_jwriter.writeResultset(DATA, rs, new String[]{entityA + "id", entityB + "id"} );
			rs.close();
			return true;
		} catch (Exception e) {
			return setError(e);
		} 
	}
	
	
	public boolean readLinked() {
		boolean rtn = false;
		try {
			String entityA =_data.optString( ENTITY_A, null);
			if ((entityA == null) || (entityA.length() == 0)) {
				setError("Link.read missing " + entityA);
				return false;
			}
			String entityB =_data.optString( ENTITY_B, null);
			if ((entityB == null) || (entityB.length() == 0)) {
				setError("Link.read missing " + entityB);
				return false;
			}
			
			if (! _data.has(ID)) {
				setError("Link.readLinked missing ID for table join");
				return false;
			}
			
			int id = _data.getInt(ID);
			
			String[] columns = null;
			if (_data.has(COLUMNS)) {
				columns = _data.getString(COLUMNS).split(",");
			}
			ResultSet rs = LinkRequest.readLinked(_dbi, entityA, entityB, id, columns);
			if (rs != null) {
				_jwriter.openArray("data");
				String[] encoded =_data.optString( ENCODED, "").split(",");			
				rtn = _jwriter.writeResultSetEncoded(rs, encoded);
				_jwriter.closeArray();
			} else {
				setError("Link.read: resultset is null");
			}

		} catch (Exception e) {
			setError(e);
			
		}finally {
			_dbi.closeAll();
		}
		return rtn;
	}
	
	public static ResultSet readLinked(DBInteract dbi, String entityA, String entityB, int id, String[] columns) {
		try {
			EntityMap emapLink = EntityMapMgr.getLinkMap(entityA, entityB);
			if (emapLink == null) {
				return null;
			}
			
			EntityMap emap = EntityMapMgr.getEMap(entityA);
			if (emap == null) {
				return null;
			}
			
			String linkTable = emapLink.getName();
			String table = emap.getName();
			
			if (columns == null) {
				columns = emap.getDefaultFields();
			}
			
			String sql = "SELECT " + emap.getFieldsCsv(columns, true) 
					+ " FROM " + table + ", " + linkTable
					+ " WHERE " + linkTable + "." + entityA + "id = " + entityA + ".id"
					+ " AND " + linkTable + "." + entityB + "id = ?";
					
			PreparedStatement ps = dbi.getPreparedStatement(sql);
			ps.setInt(1, id);
			return ps.executeQuery();

		} catch (Exception e) {
			GLogger.error(e);
			return null;
		} 
	}
	
	public boolean readComplex() {
		/*
		 * data:{
		 * 		entities:[
		 * 			{from:'file_line',to:'line',type:'linkend',where:[{fileid:333}]},
		 * 			{from:'line',to:'linepoint',type:'child'},
		 * 			{from:'linepoint',columns:[{name:'lineid',exp:'count',alias:'cnt'}]
		 * 		]
		 * 		groupby:""
		 * }
		 * 
		 * ex.
		 * 
		 * SELECT count(linepoint.lineid) as cnt
		 * FROM file_line, line, linepoint
		 * WHERE file_line.fileid = 333
		 * AND file_line.lineid = line.id
		 * AND line.id = linepoint.lineid
		 */
		try {
		
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			GLogger.error(e);
			return false;
		} 
	}
	
	/**
	 * Always returns true at this time since there is no updates possible on a 
	 * two entity link record
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean update() {
		return true;
	}

	/**
	 * 
	 * @return True if no error occurs
	 */
	@Override
	public boolean delete() {
		/*
		 * data = {
		 * 		'entitya':'<entitya_name>',
		 * 		'entityb':'<entityb_name>',
		 * 		'where': {
		 * 			'<entityb_name>id':<id>
		 * 		}
		 */

		try {
			String entityA =_data.optString( ENTITY_A, null);
			if ((entityA == null) || (entityA.length() == 0)) {
				setError("Link.delete missing " + entityA);
				return false;
			}
			String entityB =_data.optString( ENTITY_B, null);
			if ((entityB == null) || (entityB.length() == 0)) {
				setError("Link.delete missing " + entityB);
				return false;
			}
			if (! _data.has(WHERE)) {
				setError("Link.delete missing Where statement");
				return false;
			}
			String error = LinkRequest.deleteLinks(_dbi, entityA, entityB, _data.getJSONObject(WHERE));
			if (error != null) {
				setError(error);
				return false;
			} else {
				return true;
			}
		} catch (JSONException e) {
			GLogger.error(e);
			return false;
		}
	}


	public static String deleteLinks(DBInteract dbi, String entityA, String entityB, JSONObject where) {
		try {
			
			EntityMap emap = EntityMapMgr.getLinkMap(entityA, entityB);
			if (emap == null) {
				return "Link.deleteLinks invalid link entities " + entityA + ", " + entityB;
			}

			if (where == null) {
				return "Link.deleteLinks: Where statement null";
			}
			
			String[] whereNames = JSONObject.getNames(where);
			if ((whereNames == null) || (whereNames.length == 0)) {
				return "Link.deleteLinks: Where statement has no constraints";
			}

			// At this time only id fields can be used in the link deletes
			ParameterList pl = new ParameterList(emap);
			String strWhere = "";
			for (String whereName : whereNames) {
				if (strWhere.length() > 0) {
					strWhere += " AND ";					
				}
				strWhere += whereName + " = ?";
				pl.add(whereName, where.getInt(whereName));
			}
			
			String sql = "DELETE FROM " + emap.getName() + strWhere; 
			//		GLogger.debug("sql: " + sql);
			PreparedStatement ps = dbi.getPreparedStatement(sql);
			pl.setPreparedStatement(ps);
			if ( ps.execute() ) {
				return null;
			} else {
				return "Link.deleteLinks - unknown error on execution of statement";
			}
		} catch (Exception e) {
			GLogger.error(e);
			return e.getMessage();
		}			
	}
	
	public static String deleteAllLinks(DBInteract dbi, String entityA, JSONObject where) {
		try {
			if (where == null) {
				return "Link.deleteAllLinks: Where statement null";
			}
			
			String[] whereNames = JSONObject.getNames(where);
			if ((whereNames == null) || (whereNames.length == 0)) {
				return "Link.deleteAllLinks: Where statement has no constraints";
			}

			ArrayList<EntityMap> linkTables = EntityMapMgr.getLinkTables(entityA);
			for (EntityMap emap : linkTables) {
				// At this time only id fields can be used in the link deletes
				ParameterList pl = new ParameterList(emap);
				String strWhere = "";
				for (String whereName : whereNames) {
					if (strWhere.length() > 0) {
						strWhere += " AND ";					
					}
					strWhere += entityA + whereName + " = ?";
					pl.add(whereName, where.getInt(whereName));
				}
				
				String sql = "DELETE FROM " + emap.getName() + " WHERE " + strWhere; 
//						GLogger.debug("sql: " + sql);
				dbi.setLastSQL(sql);
				PreparedStatement ps = dbi.getPreparedStatement(sql);
				pl.setPreparedStatement(ps);
				ps.execute();
				ps.close();
			}
			return null;
			
		} catch (Exception e) {
			GLogger.error(e);
			return e.getMessage();
		}			
	}

	public static String deleteAllLinks(DBInteract dbi, EntityMap emapA, JSONObject where) {
		return deleteAllLinks(dbi,emapA.getName(),where);
	}

}
