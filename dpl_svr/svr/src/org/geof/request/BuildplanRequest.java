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
import org.geof.dpl.DemoBuildplan;
import org.geof.dpl.GuestObject;
import org.geof.dpl.mgr.DomainMgr;
import org.geof.request.DBRequest;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class BuildplanRequest extends DBRequest {

	public final static String ENTITYNAME = "buildplan";
	private final static String _sqlFiles = "SELECT f.* FROM File f, Buildplan_File i WHERE i.buildplanid=? AND f.id=i.fileid";
	private final static String _sqlDelFiles = "SELECT count(*) as cnt FROM Buildplan_File i WHERE i.buildplanid=? AND i.fileid=?";
	private final static String _sqlBpFiles = "SELECT fileid FROM Buildplan_File WHERE buildplanid=?";
	private final static String _sqlImgFiles = "SELECT f.* FROM File f, Buildplan bp, Image_file i WHERE bp.id=? AND bp.imageid = i.imageid AND i.fileid = f.id";

	@Override	
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} 
		else if (_actionKey.equalsIgnoreCase(READ + ".files")) {
			return readFiles();
		}
		else if (_actionKey.equalsIgnoreCase(READ + ".demo")) {
			return readDemo();
		}
		else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();
		}
		else if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();
		}
		else if (_actionKey.equalsIgnoreCase(CREATE + ".guest")) {
			return createGuest();
		}
		else if (_actionKey.equalsIgnoreCase(CREATE + ".copy")) {
			return createCopy();
		}
		else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();
		}		
		else if (_actionKey.equalsIgnoreCase(DELETE + ".file")) {
			return deleteInstallFile();
		}
		else {
			return super.process();
		}
	}
	
	public boolean createGuest() {
		GuestObject oGuest = null;
		
		try {
			DomainMgr dm = new DomainMgr(_data);
			if (dm.hasError()) {
				return setError(dm.getError());
			}			
			_data.put("session", _session);
			oGuest = new GuestObject(dm, _dbi, _data);
			oGuest.initialize(dm, _dbi, _data);
			oGuest.create();
			return true;
			
		} catch (Exception e) {
			if (oGuest != null) {
				oGuest.backout(oGuest.dirName);
			}
			return setError("DomainRequest.guestDomain failed: "+ e.getMessage());
			
		} finally {
			
			if (oGuest != null) {
				oGuest.cleanUp ();
			}
		}
	}

	public boolean createCopy() {
		try {
			JSONArray rows = _dbi.readAsJson(_entitymap, _data);
			if (rows.length() != 1) {
				return setError("DomainRequest.createCopy failed: invalid Where statement");
			}
			JSONObject flds = rows.getJSONObject(0); 
			flds.put(NAME, _fields.get(NAME));
			JSONObject data = JsonUtil.getDataFields(flds);
			_dbi.create(_entitymap, data);
			return true;
		} catch (Exception e) {
			return setError("DomainRequest.createCopy failed: "+ e.getMessage());
			
		} finally {
			
		}
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
	
	private boolean readDemo() {
		try {
			JSONArray jaDBP = DemoBuildplan.read(_dbi);
			return _jwriter.writePair(DATA,jaDBP);
		} catch (Exception e) {
			return setError(e);
		}
	}
	
	@Override
	public boolean update() {
		return super.update() ? DemoBuildplan.setIsDemo(_dbi, _data) : false;
	}
	
	@Override
	public boolean create() {
		return super.create() ? DemoBuildplan.setIsDemo(_dbi, _data) : false;
	}
		
	public static List<FileInfo> getInstallFiles(DBInteract dbi, long buildplanId) throws Exception {
		ParameterList pl = new ParameterList();
		pl.add("buildplanid", buildplanId);
		JSONArray jaIFiles = dbi.readAsJson(_sqlFiles, pl);
		
		List<FileInfo> lFiles = new ArrayList<FileInfo>();
		int count = jaIFiles.length();
		JSONObject joFile;
		for (int indx=0;indx<count;indx++) {
			joFile = jaIFiles.getJSONObject(indx);
			lFiles.add(FileRequest.getFileInfo(dbi, joFile.getLong(ID)));
		}
		return lFiles;
	}
	
	public static JSONArray getImageFilesJson(DBInteract dbi, long buildplanId) throws Exception {
		ParameterList pl = new ParameterList();
		pl.add("buildplanid", buildplanId);
		return dbi.readAsJson(_sqlImgFiles, pl);
	}

	public static List<FileInfo> getImageFiles(DBInteract dbi, long buildplanId) throws Exception {
		ParameterList pl = new ParameterList();
		pl.add("buildplanid", buildplanId);
		JSONArray jaIFiles = dbi.readAsJson(_sqlImgFiles, pl);
		
		List<FileInfo> lFiles = new ArrayList<FileInfo>();
		int count = jaIFiles.length();
		JSONObject joFile;
		for (int indx=0;indx<count;indx++) {
			joFile = jaIFiles.getJSONObject(indx);
			lFiles.add(FileRequest.getFileInfo(dbi, joFile.getLong(ID)));
		}
		return lFiles;
	}

	public boolean delete() {
		if (_where == null) {
			return setError("Missing 'Where' section of JSON");
		}
		Long buildplanid = _where.optLong("id");
		if (buildplanid == null) {
			return setError("Missing 'id' field in Where statement");
		}
		try {
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlBpFiles);
			ps.setLong(1, buildplanid);
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
	
	private boolean deleteInstallFile(){
		if (_where == null) {
			return setError("Missing 'Where' section of JSON");
		}
		Long buildplanid = _where.optLong("buildplanid");
		if (buildplanid == null) {
			return setError("Missing 'buildplanid' field in Where statement");
		}
		Long fileid = _where.optLong("fileid");
		if (fileid == null) {
			return setError("Missing 'fileid' field in Where statement");
		}
		try {
			PreparedStatement ps = _dbi.getPreparedStatement(_sqlDelFiles);
			ps.setLong(1, buildplanid);
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

	public static JSONArray getBuildPlan(DBInteract dbi, JSONObject data) throws Exception {
		EntityMap emap = EntityMapMgr.getEMap(BuildplanRequest.ENTITYNAME);
		return dbi.readAsJson(emap, data);
	}
	
	public static JSONArray getImageFiles(DBInteract dbi, Long buildplanid) throws Exception {
		if (buildplanid == null) {
			throw new Exception("buildplanid parameter is null");
		} else {
			ParameterList pl = new ParameterList();
			pl.add("buildplanid", buildplanid);
			return dbi.readAsJson(_sqlFiles, pl);
		}
	}
	

}
