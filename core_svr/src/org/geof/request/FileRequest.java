package org.geof.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.geof.data.FileInfo;
import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.job.TaskExecutor;
import org.geof.job.ThumbnailTask;
import org.geof.log.GLogger;
import org.geof.store.FileUpload;
import org.geof.store.StorageLocation;
import org.geof.store.StorageMgr;
import org.geof.util.DateUtil;
import org.geof.util.FileUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Derived Class used for managing the uploading of media files to the server
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class FileRequest extends DBRequest {

	public final static String ENTITYNAME = "file";
	
	public final static String FILENAME = "filename";
	public final static String FILEEXT = "fileext";
	public final static String FILESIZE = "filesize";
	public final static String ORIGINALNAME = "originalname";
	public final static String FILETYPE = "filetype";
	public final static String GEOMTYPE = "geomtype";
	public final static String STATUS = "status";
	public final static String CREATEDATE = "createdate";
	public final static String CHECKSUM = "checksumval";
	public final static String REGISTERDATE = "registerdate";
	public final static String REGISTEREDBY = "registeredby";
	public final static String SENDNAME = "sendname";
	public final static String NOTES = "notes";
	public final static String STORAGELOCID = "storagelocid";

	public static final String FIELDLIST = "storagelocid, status, filename, fileext, filetype, originalname";
	private static final String _sqlChangeName = "UPDATE file SET filename = ? WHERE id = ?";

	public static final int OFFLINE = 0, ONLINE = 1, UPLOADING = 2, UPLOAD_FAILED = 3;

	private static final String _sqlFile = "SELECT * FROM File WHERE id=?";
	/**
	 * Overrides the base class's process() call to handle all file upload actions.
	 * 
	 * @return True if the method a possible Action was found.
	 */
	@Override
	public boolean process() {

		if (_actionKey.equalsIgnoreCase(CREATE)) {
			return create();

		} else if (_actionKey.equalsIgnoreCase(UPDATE)) {
			return update();

		} else if (_actionKey.equalsIgnoreCase(DELETE)) {
			return delete();

		} else if (_actionKey.equalsIgnoreCase(READ)) {
			return super.read();
			
		} else if (_actionKey.equalsIgnoreCase(UPDATE + ".thumbnail")) {
			return buildThumbnails();
			
		} else if (_actionKey.equalsIgnoreCase(UPDATE + ".addlink")) {
			return addLink();
		} else if (_actionKey.equalsIgnoreCase(UPDATE + ".removelink")) {
			return removeLink();
		}
		return false;
	}

	public boolean create() {

		try {
			if (!_data.has("fields")) {
				return setError("FileRequest.request Data missing Fields");
			}
			JSONObject fields = _data.getJSONObject("fields");
			String sendname = fields.getString("sendname");

			int fileindex = fields.getInt("fileindex");
			String fpart = fields.getString("filepart");
			boolean rtn = StorageMgr.instance().writeFilePart(fpart, fileindex, sendname);
			_jwriter.openObject("data");
			_jwriter.writePair("sendname", sendname);
			_jwriter.writePair("fileindex", fileindex);
			_jwriter.writePair(_action, rtn);
			_jwriter.closeObject();
			return true;
		} catch (JSONException e) {
			return setError(e);
		}
	}
	
	public static boolean registerRecord(DBInteract dbi, FileUpload fu) throws Exception {
		EntityMap emap = EntityMapMgr.getEMap(ENTITYNAME);
		if (emap == null) {
			return false;
		}
		fu.Status  = UploadRequest.REGISTERED;
		JSONObject data = new JSONObject();
		JSONObject fields = fu.toJsonObject(true);
		fields.put(REGISTERDATE,DateUtil.nowStr());
		data.put("fields", fields);
		boolean rtn = dbi.create(emap, data);
		if (rtn) {
			JSONObject newfile = dbi.getLastReturning(); 
			fu.fileid = newfile.getLong("id");
		} else {
			fu.Status  = UploadRequest.ERROR;
			//TODO: set error - probably from DBI
		}
		return rtn;
	}
	

	/**
	 * Changes the file's filename field. this is due for security reasons since
	 * the sendname on uploads is known to the user and is changed once the upload
	 * process is complete
	 * 
	 * @param id Primary key of the record.
	 * @param newName The new name of the file as saved to the working location.
	 * @return returns error message if error occurs
	 */
	public static String changeName(DBInteract dbi, long id, String newName) throws Exception {
		PreparedStatement ps = dbi.getPreparedStatement(_sqlChangeName);
		ps.setString(1, newName);
		ps.setLong(2, id);
		ps.execute();			
		return null;
	}

	/**
	 * Method stub to be completed later
	 * 
	 * @return Always returns true for now
	 */
	public boolean delete() {
		if (! _where.has(ID)) {
			setError("FileRequest.delete 'id missing from WHERE'");
			return false;
		}
		Long fileid = _where.optLong(ID);
		if (fileid == null) {
			setError("FileRequest.delete 'Where' object missing id field");
		}
		String error = FileRequest.delete(_dbi, fileid);
		if (error != null) {
			return setError(error);
		} else {
			return true;
		}
	}

	public static String delete(DBInteract dbi, long fileid) {
		try {
			EntityMap emap = EntityMapMgr.getEMap(FileRequest.ENTITYNAME);
			FileInfo fi = FileRequest.getFileInfo(dbi,fileid);
			if (fi == null) {
				return "FileRequest.delete - no file info for file id: " + fileid;
			} else {
				/*
				 * 1) mark the file as offline to restrict access during removal
				 * 2) remove the file(s) from the storage location
				 * 3) remove links to an assoicative tables.
				 * 4) remove child records
				 * 5) remove the file record from the database
				 */
					
				// step 1
				if ( ! FileInfo.setStatus(fi.ID, OFFLINE, dbi, emap)) {
					GLogger.debug("FileRequest.delete - cannot set status for file id: " + fileid);
//					return false;
				}

				String[] filepaths;
				if (fi.FileType == FileUtil.PHOTO) {
					filepaths = new String[FileUtil.THUMBEXT.length];
					// create list of all files based on extentions
					for (int indx = 0; indx < filepaths.length; indx++) {
						filepaths[indx] = fi.Fullpath + FileUtil.THUMBEXT[indx];
					}
				} else {
					filepaths = new String[] {fi.Fullpath};
				}
				
				// step 2
				StorageMgr.instance().removeFiles(filepaths, dbi);
			}

			// step 3
			dbi.deleteLinks( FileRequest.ENTITYNAME, fileid);

			// step 4
			//TODO: add code to remove all child records
			
			// step 5
			JSONObject json = new JSONObject();
			json.put(ID,fileid);
			dbi.delete(emap, json);
			return dbi.getError();	
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public boolean update() {
		try {
			boolean rtn = super.update();
			_jwriter.openObject(Request.DATA);
			_jwriter.writePair(FIELDS, _fields);
			_jwriter.writePair(_action, rtn);
			_jwriter.closeObject();
			return true;
		} catch (Exception e) {
			setError(e);
			return false;
		}
	}

	public FileInfo getFileInfo( long id ) {
		
		String error = null;
		FileInfo fi = FileRequest.getFileInfo(_dbi, id ); 
		if (fi == null) {
			setError(error);
		}
		return fi;
	}
	
	public static FileInfo getFileInfo(DBInteract dbi, long id ) {
		ResultSet rs = null;
		FileInfo fi = null;
		try {
			PreparedStatement ps = dbi.getPreparedStatement(_sqlFile);
			ps.setLong(1, id);
			rs = ps.executeQuery();			
			if ( rs != null && rs.next() ) {
				String filename = rs.getString(FILENAME);
				long storageid = rs.getLong(STORAGELOCID);

				String systemDir = "";
				if (storageid > -1) {
					StorageLocation sl = StorageMgr.instance().getById(storageid);
					if (sl == null) {
						GLogger.error("FileRequest.getFileInfo - StorageLocation missing for " + storageid);
					} else {
						systemDir = sl.systemdir;
					}
				}
				fi = new FileInfo(id, filename, rs.getString(FILEEXT), rs.getInt(FILETYPE),
						systemDir + "/" + filename, rs.getString(ORIGINALNAME),
						systemDir, rs.getInt(STATUS), 0, storageid);
				
			}
		} catch (Exception e) {
			GLogger.error( "FileRequest.getFileInfo - " + e.getMessage());
		} finally {
			if (rs != null) {
				try { rs.close(); } catch (Exception e2){}
			}
		}
		return fi;
	}
	
	private boolean buildThumbnails() {
		try {
			long fileid = _where.optLong( ID, -1);
			if (fileid == -1) {
				setError("Missing id for file to thumbnail");
				return false;
			}
			TaskExecutor.execute(new ThumbnailTask(fileid));
			return true;
		} catch (Exception e) {
			setError(e.getMessage());
			return false;
		}
	}

	private boolean addLink() {
		try {
			if (_fields == null) {
				setError("FileRequest.addLink Data missing Fields");
				return false;
			}
			String table_a = ENTITYNAME;
			String table_b = _fields.optString("to_table");
			long fileid = _fields.optLong("fileid");
			long toTableId = _fields.optLong(table_b + ID);
			boolean rtn = _dbi.addLink(table_a,  fileid, table_b, toTableId);
			if (! rtn) {
				setError(_dbi.getError());
			}
			return rtn;
		} catch (Exception e) {
			GLogger.error(e);
			setError(e.getMessage());
			return false;
		}
	}
	
	private boolean removeLink() {
		boolean rtn = false;
		
		return rtn;
	}
	
}
