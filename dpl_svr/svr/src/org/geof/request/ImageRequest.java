package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.geof.data.FileInfo;
import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.db.ParameterList;
import org.geof.dpl.DomainData;
import org.geof.request.DBRequest;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class ImageRequest extends DBRequest {

	private final static String ENTITYNAME = "image";

	private final static String _sqlFiles = "SELECT f.* FROM File f, Image_File i WHERE i.imageid=? AND f.id=i.fileid";
	private final static String _sqlDelFiles = "SELECT count(*) as cnt FROM Image_File i WHERE i.imageid=? AND i.fileid=?";
	private final static String _sqlImageFiles = "SELECT fileid FROM Image_File WHERE imageid=?";
	
	private final static String[] _sqlClean = {
											"DELETE FROM image WHERE name NOT IN (SELECT name FROM domain)",
											"DELETE FROM image_file WHERE imageid NOT IN (SELECT id FROM image)",
											"DELETE FROM file WHERE id NOT IN (SELECT fileid FROM image_file)"
											};

	
	@Override	
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} else if (_actionKey.equalsIgnoreCase(READ + ".files")) {
			return readFiles();
		}
		else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		}
		else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		}
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		}
		else if (_actionKey.equalsIgnoreCase(DELETE + ".file")) {
			return deleteImageFile();
		}
		else {
			return super.process();
		}
	}
	
	@Override
	/**
	 * 
	 */
	public boolean create() {
		boolean rtn = true;
		
		return rtn;
	}
	
	public static JSONObject createFromDomainData(DBInteract dbi, DomainData dd) throws Exception {
		JSONObject data = JsonUtil.getDataFields();
		JSONObject flds = data.getJSONObject(FIELDS);
		flds.put(NAME, dd.name);
		//TODO: change this to status and add Enumeration for 1
		flds.put("statusid", 1);
		flds.put("descriptions", "Created from Guest Domain: " + dd.name);
		return dbi.create(ENTITYNAME, data);
	}
	
	private boolean readFiles(){
		if (_where == null) {
			return setError("Missing 'Where' section of JSON");
		}
		Long imageid = _where.optLong(ID);
		if (imageid == null) {
			return setError("Missing 'id' field in Where statement");
		}
		try {
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlFiles);
			ps.setLong(1, imageid);
			ps.execute();			
			_dbi.read(ps);
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}
	
	public static JSONArray getFilesJson(DBInteract dbi, Integer imageid) throws Exception {
		if (imageid == null) {
			throw new Exception("imageid parameter is null");
		} else {
			ParameterList pl = new ParameterList();
			pl.add("imageid", imageid);
			return dbi.readAsJson(_sqlFiles, pl);
		}
	}

	public static List<FileInfo> getFiles(DBInteract dbi, long imageid) throws Exception {
		ParameterList pl = new ParameterList();
		pl.add("buildplanid", imageid);
		JSONArray ja = dbi.readAsJson(_sqlFiles, pl);
		
		List<FileInfo> lFiles = new ArrayList<FileInfo>();
		for (int indx=0;indx<ja.length();indx++) {
			lFiles.add(FileRequest.getFileInfo(dbi, ja.getJSONObject(indx).getLong(ID)));
		}
		return lFiles;
	}
	
	private boolean deleteImageFile(){
		if (_where == null) {
			return setError("Missing 'Where' section of JSON");
		}
		Long imageid = _where.optLong("imageid");
		if (imageid == null) {
			return setError("Missing 'imageid' field in Where statement");
		}
		Long fileid = _where.optLong("fileid");
		if (fileid == null) {
			return setError("Missing 'fileid' field in Where statement");
		}
		try {
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlDelFiles);
			ps.setLong(1, imageid);
			ps.setLong(2, fileid);
			ps.execute();
			ResultSet rs = ps.executeQuery();
			if ( rs.next()) {
				if (rs.getInt(1) == 1) {
					FileRequest.delete(_dbi, fileid);
				}
			}
			rs.close();
			return true;
		} catch (Exception e) {
			return setError(e.getMessage());
		}
	}

	@Override
	public boolean delete() {
		if (_where == null) {
			return setError("Missing 'Where' section of JSON");
		}
		Long imageid = _where.optLong("id");
		if (imageid == null) {
			return setError("Missing 'id' field in Where statement");
		}
		try {
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlImageFiles);
			ps.setLong(1, imageid);
			ps.execute();
			ResultSet rs = ps.executeQuery();
			if ( rs.next()) {
				FileRequest.delete(_dbi, rs.getLong(1));
			}
			rs.close();
			_dbi.delete(_entitymap, _where);
			return true;
		} catch (Exception e) {
			return setError(e.getMessage());
		}
	}

	public static boolean deleteDb(DBInteract dbi, JSONObject where) throws Exception {
		if (where == null) {
			throw new Exception("Missing 'Where' section of JSON");
		}
		Long imageid = where.optLong(ID, -1);
		if (imageid == -1) {
			throw new Exception("Missing 'id' field in Where statement");
		}
		
		PreparedStatement ps = dbi.getPreparedStatement(_sqlImageFiles);
		ps.setLong(1, imageid);
		ps.execute();
		ResultSet rs = ps.executeQuery();
		if ( rs.next()) {
			EntityMap emapFile = EntityMapMgr.getEMap("file");
			dbi.delete(emapFile, JsonUtil.getWhere(ID, rs.getLong(1)));
		}			
		rs.close();
		dbi.delete(EntityMapMgr.getEMap(ENTITYNAME), where);
		return true;
	}

	public static boolean deleteDbByName(DBInteract dbi, String domainname) throws Exception {
		JSONObject where = JsonUtil.getWhere(NAME, domainname);
		dbi.delete(EntityMapMgr.getEMap(ENTITYNAME), where);
		return true;
	}
	
	public static boolean deleteDbById(DBInteract dbi, long imageid) throws Exception {
		JSONArray jaFile = dbi.getLinks("image", imageid, "file");
		for (int iFile=0;iFile<jaFile.length();iFile++) {
			long fileid = jaFile.getJSONObject(iFile).getLong("fileid");	
			FileRequest.delete( dbi, fileid);
		}

		return dbi.delete(EntityMapMgr.getEMap(ENTITYNAME), JsonUtil.getWhere(ID, imageid));
	}
	
	public static boolean removeOrphans(DBInteract dbi){
		return dbi.execute(_sqlClean[1]);
	}
}
