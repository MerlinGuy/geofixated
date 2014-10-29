package org.geof.tasker;

import java.io.File;
import java.util.ArrayList;

import org.geof.data.FileInfo;
import org.geof.db.DBInteract;
import org.geof.job.TaskExecutor;
import org.geof.job.ThumbnailTask;
import org.geof.log.GLogger;
import org.geof.request.FileRequest;
import org.geof.request.GenericRequest;
import org.geof.request.KeywordRequest;
import org.geof.service.AuthCodeMgr;
import org.geof.service.AuthorityMgr;
import org.geof.service.SessionMgr;
import org.geof.service.GSession;
import org.geof.store.StorageLocation;
import org.geof.store.StorageMgr;
import org.geof.util.DateUtil;
import org.geof.util.FileUtil;
import org.geof.util.GeoUtil;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class PulleyTask extends ARetrieveTask implements Runnable {

	private boolean OVERRIDE = true;
	public static final String FILE = "file";
	public static final String FILE_NAME = "filename";
	public static final String AUTH_CODE = "auth_code";
	public static final String USRID = "usrid";
	public static final String EMAIL = "email";
	public static final String ORG_NAME = "originalname";
	public static final String TYPE = "type";
	public static final String FILE_SIZE = "filesize";
	public static final String FILE_EXT = "fileext";
	public static final String CREATE_DATE = "createdate";
	public static final String CHECKSUM = "checksumval";
	public static final String NOTES = "notes";
	public static final String DOCUMENT = "document";
	
	public static final String STATUS = "status";
	public static final String STORAGE_LOC_ID = "storagelocid";
	public static final String REGISTER_DATE = "registerdate";
	public static final String REGISTERED_BY = "registeredby";
	public static final String TAG = "tag";
	public static final String SAVED_DIRECTORY = "savedirectory";
	
	
	public static final String MISSING_FILENAME = "JSON Missing filename";
	public static final String MISSING_ORG_NAME = "JSON Missing Original Filename for: %s";
	public static final String MISSING_AUTH_CODE = "Invalid / Missing auth_code: %s, orgName: %s";
	public static final String MISSING_USRID = "Invalid Or Missing usrid for %s, orgName: %s";
	public static final String CANNOT_MOVE_FILE = "Can not move staged file: %s%s";
	
	public static final String INVALID_AUTH_CODE = "Usrid %d does not valid auth_code: %s";
	public static final String MISSING_PERMISSION = "Usrid %d does not have %s permission for %s";
	
	protected JSONArray _downloads = null;
	
	public PulleyTask() {}

//	public boolean doTask() {
//		for (ARetrieveTask prt : _tasks) {
//			if (prt.paused()) {
//				JSONArray jaPFI = new JSONArray();
//				jaPFI = prt.retrieve();
//				if (jaPFI.length() > 0) {
//					processReturn(jaPFI, prt);
//				}
//			}
//		}
//		// TODO: call the Store manager to register and move the file.
//		return true;
//	}

	protected void processDownloads() {
		int length = _downloads.length();
//		GLogger.debug("processReturn file count " + length);
		DBInteract dbi = null;
		String downloaddir = this.get(ARetrieveTask.DOWNLOAD_DIR).toString();
		try {
			dbi = new DBInteract();
			for (int indx = 0; indx < length; indx++) {
				JSONObject joFile = _downloads.getJSONObject(indx);
				if ( ! processFile(joFile, dbi)) {
					// clean up the downloaded file on failed processing.
					String fileName = joFile.optString( FILE_NAME, null);
					if (fileName != null) {
						String filePath = downloaddir + "/" + fileName;
						GLogger.error("Deleteing unprocessed file: %s", filePath);
						File file = new File(filePath);
						if (file.exists()) {
							file.delete();
						}
					}
				}
			}
		} catch (Exception e) {
			GLogger.error(e);
		}
		if ( dbi != null) {
			dbi.releaseConnection();
		}
	}

	private boolean processFile(JSONObject json, DBInteract dbi) {
		try {
			if (! JsonUtil.check(json, FILE_NAME, MISSING_FILENAME, (Object[])null)) {
				return false;
			}
			String filename = json.getString(FILE_NAME).trim();
			
			if (! JsonUtil.check(json, ORG_NAME, MISSING_ORG_NAME, filename)) {
				return false;
			}
			String orgName = json.getString(ORG_NAME).trim();
			String file_ext = FileUtil.getExtention(orgName);
			
			if (! JsonUtil.check(json, AUTH_CODE, MISSING_AUTH_CODE, filename, orgName)) {
				return false;
			}
			String auth_code = json.getString(AUTH_CODE).trim();

			if (! JsonUtil.check(json, USRID, MISSING_USRID, filename, orgName)) {
				return false;
			}
			long usrid = json.getLong(USRID);

			if (!AuthCodeMgr.hasValidCode(auth_code, usrid)) {
				GLogger.error(INVALID_AUTH_CODE, usrid, auth_code);
				return false;
			}

			String stageDir = json.getString(SAVED_DIRECTORY);
			
			ArrayList<Integer> ugroups = AuthorityMgr.getUGroups(dbi, usrid);
			GSession session = SessionMgr.createSession(usrid,ugroups);
			if ( ! AuthorityMgr.hasPermission(session, FILE, AuthorityMgr.CREATE)) {
				GLogger.error(MISSING_PERMISSION, usrid, AuthorityMgr.CREATE, FILE);
				return false;
			}

			String media_table = null;
			if (json.has(TYPE)) {
				media_table = json.optString( media_table, null);
			} 
			if (media_table == null) {
				media_table = FileUtil.getFileType(file_ext,DOCUMENT);
				json.put(media_table, media_table);
			}

			int filetype = FileUtil.getIdByTypeName(media_table);
			int filesize = json.getInt(FILE_SIZE);

			StorageLocation sloc = StorageMgr.instance().getLocation(filetype, filesize);

			boolean moved = StorageMgr.instance().moveFromStaged(stageDir, sloc.systemdir, filename);
			if (!moved) {
				GLogger.error(CANNOT_MOVE_FILE, stageDir, filename);
				return false;
			}
			JSONObject fields = new JSONObject();
			fields.put(FILE_NAME, filename);
			fields.put(FILE_EXT, file_ext);
			fields.put(FILE_SIZE, filesize);
			fields.put(ORG_NAME, orgName);
			fields.put(STATUS, FileInfo.OFFLINE);
			fields.put(STORAGE_LOC_ID, sloc.storageid);
			fields.put(REGISTER_DATE, DateUtil.nowStr());
			fields.put(REGISTERED_BY, json.getInt(USRID));
			fields.put(CHECKSUM, json.getString(CHECKSUM));
			fields.put(NOTES, json.optString( NOTES,""));

			long fileid = -1;

			int count = fileCount(json, "filesize,originalname,checksumval", dbi);
			if (count > 0) {
				// add code so that the email is deleted
				return false;
			}

			File file = new File(sloc.systemdir + "/" + filename);
			
			// create a point|line|poly if possible and return geomid, geomtype 
			JSONObject jGeometry = GeoUtil.saveGeometryToDb(filetype, file, file_ext, dbi);
			JsonUtil.copyTo(jGeometry, fields, OVERRIDE);
			String createDate = jGeometry.getString("utcdate");
			if (createDate == null) {
				createDate = DateUtil.nowStr();
			}
			
			fields.put(CREATE_DATE, createDate);
			JSONObject data = JsonUtil.getDataFields(fields);
			JSONObject keys = GenericRequest.create(FileRequest.ENTITYNAME, data, dbi);
			fileid = keys.getInt("fileid");
			if (filetype == FileUtil.PHOTO) {
				TaskExecutor.execute(new ThumbnailTask(fileid));
			}
			
			if (fileid > -1) {
				StorageMgr.instance().update(dbi, sloc.storageid);
				
				// link the keywords to the newly created media record.
				String[] keywords = json.optString(TAG, "").split(",");
				for (String keyword : keywords ){
					linkKeyword(FILE, fileid, keyword, false, dbi);
				}
			}
			return true;
		} catch (Exception e) {
			GLogger.error(e);
			return false;
		}
	}
	
	private boolean linkKeyword(String table, long media_id, String keyword,  boolean decode, DBInteract dbi) throws Exception {
		long keyid = KeywordRequest.getID(keyword, "", 1, true, decode, dbi);
		dbi.addLink(table, media_id, "keyword", keyid);
		return true;
	}

	private int fileCount(JSONObject json, String keylist, DBInteract dbi) {
		try {
			JSONObject where = JsonUtil.copyFrom(json, null, keylist);
			JSONObject data = JsonUtil.getDataWhere(where);
			data.put(DBInteract.COLUMNS, "id");
			JSONArray results = GenericRequest.read(FILE, data, dbi);
			return (results == null) ? -1 : results.length();
		} catch (Exception e) {
			GLogger.error(e);
			return -1; 
		}
	}
	
	public boolean hasFile(JSONObject json) {
		DBInteract dbi = new DBInteract();
		int count = fileCount( json, "filesize,originalname,checksumval", dbi);
		boolean hasFile = count > 0;
		dbi.releaseConnection();
		return hasFile;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	public void dispose() {
	}

}
