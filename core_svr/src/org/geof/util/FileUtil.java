package org.geof.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.geof.data.FileInfo;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.log.GLogger;
import org.geof.request.FileRequest;
import org.geof.service.AuthorityMgr;
import org.geof.service.BaseContextListener;
import org.geof.service.GSession;
import org.geof.service.SessionMgr;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileUtil {
	
    public static int UNKNOWN = -1, PHOTO = 0, VIDEO = 1, AUDIO = 2, DOCUMENT = 3, SHAPE = 4, TRACK = 5;  
	public static String[] FILETYPES = { "photo", "video", "audio", "document", "shape", "track" };
	public static String FILETYPE_ENUM = "0,1,2,3,4,5";
	public static final String[] THUMBEXT = { "", "_0", "_1", "_2", "_3", "_4", "_5" };

	public static final int CT_IMAGE=0, CT_HTML=1, CT_WORD=2, CT_EXCEL=3, CT_TEXT=4, CT_PDF=5, CT_JSON=6;
	public static String[] CONTENTTYPES = { "image/jpeg", "text/html", "application/vnd.ms-word", "Application/vnd.ms-excel", "text/plain", "application/pdf","application/json" };
	private static final int BLOCKSIZE = 4048;
//	private static final String APPJSON = "application/json";
	private static final String HTML = "text/html";
	private static final String ID = "id";
	private static final String SESSIONID = "sessionid";

	
	private static HashMap<String,String> _mapFileTypes = new HashMap<String,String>(); 

	public static String getExtention(File file) {
		return getExtention(file.getName());
	}

	public static String getExtention(String filename) {
		int indx = filename.lastIndexOf(".");
		return (indx == -1) ? "" : filename.substring(indx + 1);
	}

	public static String getPrefix(File file) {
		String name = file.getName();
		int indx = name.indexOf(".");
		return (indx == -1) ? "" : name.substring(0, indx);
	}

	/*
	 * Get the extension of a file.
	 */
	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}
	
	public static boolean loadFileTypes(String confPath) {
		try {
			if (! (new File(confPath)).exists() ){
				GLogger.error("File Types conf missing: %s", confPath);
				return false;
			}
			JSONObject json = JsonUtil.parseConfigFile(confPath);
			if (json == null) {
				GLogger.error("Error parsing File Types file: %s", confPath);
				return false;
			}
			_mapFileTypes.clear();
			JSONArray filetypes = json.getJSONArray("filetypes");
			int length = filetypes.length();
			for (int indx=0; indx<length; indx++) {
				JSONObject filetype = filetypes.getJSONObject(indx);
				String key = filetype.getString("type");
				String[] exts = filetype.getString("extensions").split(",");
				for (String ext : exts) {
					_mapFileTypes.put(ext, key);
				}
			}
			return true;
		} catch (Exception e) {
			GLogger.error(e);
		}
		return false;
	}
	
	/**
	 * Returns the filetype id 		
	 * @param filename whose extension will be used to get the filetype id
	 * @return Filetype Id
	 */
	public static int getIdByFilename(String filename) {
		String extension = getExtention(filename);
		String typename = getFileType(extension, null);
		if (typename == null) {
			return UNKNOWN;
		}
		return getIdByTypeName(typename);
	}
	
	/**
	 * Returns the filetype id 		
	 * @param extension that will be used to get the filetype id
	 * @return Filetype Id
	 */
	public static String getFileTypeByExtension(String extension) {
		return getFileType(extension, null);
	}
	
	/**
	 * Returns the filetype id 		
	 * @param extension that will be used to get the filetype id
	 * @param value Default value to return if no id is found.
	 * @return Filetype Id
	 */
	public static String getFileType(String extension, String value) {
		extension = extension.toLowerCase().trim();
		if (_mapFileTypes.containsKey(extension)) {
			return _mapFileTypes.get(extension);
		} else {
			return value;
		}
	}
	
	/**
	 * Returns the filetype id by a lookup into the FILETYPES list		
	 * 
	 * @param typeName
	 * @return Filetype Id
	 */
	public static int getIdByTypeName(String typeName) {
		if (typeName == null) {
			return -1;
		}
		return Arrays.asList(FILETYPES).indexOf(typeName.toLowerCase());
	}

    public static String parseFile(String path) throws FileNotFoundException {
    	Scanner scanner = new Scanner(new File(path)); 
    	String file = scanner.useDelimiter("\\Z").next();
    	scanner.close();
    	return file;
    }
    
	public static void sendImage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ServletOutputStream outstream = null;
		FileInputStream fis = null;
		String fullpath = null;
		String typeName = null;
		long fileid = -1;
		String contentType = CONTENTTYPES[CT_HTML];
		
		try {
			fileid = Long.parseLong(request.getParameter(ID));
			String sessionkey = request.getParameter(SESSIONID);
			
			if (sessionkey == null) {
				writeHtmlError(response,"Session id was not sent in query string.");
				return;
			}

			GSession session = SessionMgr.getSession(sessionkey);
			if (session == null) {
				writeHtmlError(response,"Session key sent is not valid (expired?).");
				return;
			}
			
			if (!AuthorityMgr.hasPermission(session, "file", "read")) {
				writeHtmlError(response,"User does not have read permission for " + typeName + " Request.");
				return;
			}
			
			

			EntityMap emap = EntityMapMgr.getEMap(FileRequest.ENTITYNAME);
			FileInfo finfo = FileInfo.getInfo(fileid, null);
			
			String strSize = request.getParameter("size");
			int size = strSize != null ? Integer.parseInt(strSize) : 0;
			String filepath = "";
			File file = null;
			
			boolean useMissing = (finfo == null || finfo.Status == FileInfo.OFFLINE);
			if (! useMissing ) {
				if (finfo.FileType == FileUtil.PHOTO) {				
					finfo.Size = size;
					filepath = finfo.Fullpath + FileUtil.THUMBEXT[size];
					file = new File(filepath);
					contentType = CONTENTTYPES[CT_IMAGE];
					if (!file.exists()) {
						FileInfo.setStatus(fileid, FileInfo.OFFLINE, null, emap);
						useMissing = true;
					}

				} else if (finfo.FileType == FileUtil.DOCUMENT) {
					if (finfo.Fileext.equalsIgnoreCase("doc")) {
						contentType = CONTENTTYPES[CT_WORD];
						
					} else if (finfo.Fileext.equalsIgnoreCase("xl")) {
						contentType = CONTENTTYPES[CT_EXCEL];
						
					} else if (finfo.Fileext.equalsIgnoreCase("txt")) {
						contentType = CONTENTTYPES[CT_TEXT];
					}
					response.addHeader("content-disposition", "attachment; filename=" + finfo.OriginalName);
					
				} else {
					useMissing = true;
				}
			}
			if ( useMissing ){
				filepath = BaseContextListener.CONF_PATH + "missing.png";
				file = new File(filepath);
				contentType = CONTENTTYPES[CT_IMAGE];
			}

			int contentLength = (int) file.length();
			fis = new FileInputStream(file);

			outstream = response.getOutputStream();
			response.setContentType(contentType);
			response.setContentLength(contentLength);
			
			byte[] buffer = new byte[BLOCKSIZE];
			int bytesread = 0;
			while ((bytesread = fis.read(buffer)) != -1) {
				outstream.write(buffer, 0, bytesread);
			}

		} catch (Exception e) {			
			GLogger.error("File id: " + fileid + ", path: " + fullpath);
			GLogger.error("Thumbnail.sendImage: " + e.getMessage());
			response.getOutputStream().close();

		} finally {
			try {
				fis.close();
			} catch (Exception e) {}
			try {
				outstream.close();
			} catch (Exception e) {}
		}
	}
	
	private static void writeHtmlError(HttpServletResponse response, String error) {
		try {
			response.setContentType(HTML);
			PrintWriter out = response.getWriter();
			out.println("{\"error\":\"" + error + "\"}");
			out.close();
		} catch (IOException ioe) {
			GLogger.error( ioe );
		}
	}

	public static String getStream(InputStream is) {
		Scanner s = new Scanner(is);
		s.useDelimiter("\\A");
		String rtn = s.hasNext() ? s.next() : "";
		s.close();
		return rtn;
	}
	
	//---------------------------------
	public static String makeTargetDir(String directory) {
		return makeTargetDir(new File(directory));
	}
	
	public static String makeTargetDir(File file) {
	    if (file.exists()) {
	        return "File exists";
	    } else {
	        file.mkdir();
	        return null;
	    }
	}
	
	public static String endPath(String path) {
		if (path.lastIndexOf('/') != path.length() -1) {
			path += "/";
		}
		return path;
	}
	
	//---------------------------------
	public static File makeDirStruct(String path, String directory) throws Exception  {
		path = endPath(path) + directory;			
		
		File new_dir = new File(path);
		if (!new_dir.exists()) {
			new_dir.mkdirs();
		}
		if (!new_dir.exists()) {
			throw new Exception( "Could not create working directory: " + path);
		}
		return new_dir;
	}
	
	public static boolean removeDirectory(String path) {
		if (path != null && path.length() > 0) {
			return removeDirectory( new File(path) );
		}
		return true;
	}

	public static boolean removeDirectory(File file) {
		if (file != null) {
			if (file.exists()) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					return false;
				}
			}
		}
		return true;
	}

}
