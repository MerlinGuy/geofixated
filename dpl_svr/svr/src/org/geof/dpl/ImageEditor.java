package org.geof.dpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geof.log.GLogger;
import org.geof.util.FileUtil;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import com.redhat.et.libguestfs.*;

import java.util.Map;

public class ImageEditor {

	public static final int SEARCH = 0;
	public static final int REPLACE = 1;
	private List<String> _errors = new ArrayList<String>();
	String _error = null;
	int _tempIndx = 0;
	String nl = System.getProperty("line.separator");

	// -------------------------------
	// Modify both the image file and the xml file
	// public HashMap<String,String> modify(JSONObject mod_cfg, JSONObject
	// changes, JSONObject vargs) {
	public HashMap<String, String> modify(GuestObject oClone) {
		HashMap<String, String> rtn = new HashMap<String, String>();
		try {

			// TODO: change this to rotate through all image files
			// sent : to find the correct one.
			ImageFile[] imgFiles = oClone.imageFiles;
			String img = imgFiles[0].guest_image;
			GuestFS g = this.mount_disk(img);
			if (g == null) {
				GLogger.error("error", "Could not mount disk - " + img);
				rtn.put("error", "Could not mount disk - " + img);
				return rtn;
			}

			GLogger.debug("Mounted image OS: " + img);

			String workDir = FileUtil.endPath(oClone.workDirectory);

			GLogger.debug("Creating uploads: workDir " + workDir);
			JSONObject imageChanges = oClone.imageChanges;
			JSONArray uploads = imageChanges.optJSONArray("upload");
			if (!this.create_uploads(oClone)) {
				g.umount_all();
				rtn.put("error", "Could not modify upload files");
				return rtn;
			}
			GLogger.debug("... upload files created, ready for upload");

			for (int indx = 0; indx < uploads.length(); indx++) {
				JSONObject joFile = uploads.getJSONObject(indx);
				String target = joFile.optString("target");
				String newfilename = workDir + target;
				String in_image_file = joFile.getString("guestdir") + target;
				try {
					g.upload(newfilename, in_image_file);
				} catch (Exception e) {
					_errors.add("ImageEditory uploads - file: " + in_image_file + ", " + e.getMessage());
				}
			}
			GLogger.debug("... upload files added to image");

			JSONArray appends = imageChanges.optJSONArray("append");
			for (int indx = 0; indx < appends.length(); indx++) {
				JSONObject joApnd = appends.getJSONObject(indx);
				JSONArray jaEdits = joApnd.optJSONArray("edits");
				String filepath = joApnd.optString("filepath");
				String add_line = joApnd.optString("add_line");
				this.append_line(g, filepath, add_line, jaEdits);
			}
			GLogger.debug("... file appendeds made to image");

			JSONArray edits = imageChanges.optJSONArray("edit");
			for (int indx = 0; indx < edits.length(); indx++) {
				JSONObject joEdit = edits.getJSONObject(indx);
				String filepath = joEdit.optString("filepath");
				JSONArray jaEdits = joEdit.optJSONArray("edits");
				this.edit_file(g, filepath, jaEdits);
			}
			GLogger.debug("... file edits made to image");

			g.umount_all();
		} catch (Exception e) {
			GLogger.error(e);
			rtn.put("error", e.getMessage());
		}
		return rtn;
	}

	// -------------------------------
	// Runs the all the modifying code
	public GuestFS mount_disk(String imgName) {
		try {
			GLogger.debug("mounting image - " + imgName);
			File file = new File(imgName);
			if (!file.exists()) {
				this._error = "Image file .get(" + imgName + ") does not exist";
				return null;
			}

			Runtime.getRuntime().exec("chmod 777 " + imgName);
			GuestFS g = new GuestFS();
			HashMap<String, Object> opts = new HashMap<String, Object>();
			opts.put("readonly", Boolean.FALSE);
			g.add_drive_opts(imgName, opts);
			try {
				g.launch();

			} catch (LibGuestFSException le) {
				GLogger.error(le.getMessage());
				return null;
			}
			String[] roots = g.inspect_os();

			if (roots.length == 0) {
				this._error = "modifyImage: no operating systems found for " + imgName;
				return null;
			}

			for (String root : roots) {
				Map<String, String> mps = g.inspect_get_mountpoints(root);
				List<String> mps_keys = new ArrayList<String>(mps.keySet());
				// Collections.sort (mps_keys, COMPARE_KEYS_LEN);

				for (String mp : mps_keys) {
					String dev = mps.get(mp);
					try {
						g.mount(dev, mp);
					} catch (Exception exn) {
						System.err.println(exn + " (ignored)");
					}
				}

			}
			GLogger.debug("...image mounted ");
			return g;
		} catch (Exception e) {
			GLogger.debug(e);
			return null;
		}
	}

	// -------------------------------
	// uploads each file to image
	private boolean create_uploads(GuestObject oClone) {
		boolean noErrors = true;
		try {
			String tmplDir = oClone.templateDirectory;
			String workDir = FileUtil.endPath(oClone.workDirectory);
			JSONArray uploads = oClone.imageChanges.getJSONArray("upload");

			int len = uploads.length();

			String[] keys = oClone.getFieldKeys();

			for (int iUpld = 0; iUpld < len; iUpld++) {
				JSONObject joUpload = uploads.getJSONObject(iUpld);
				String infile = tmplDir + joUpload.optString("source");
				String outfile = workDir + joUpload.optString("target");
				BufferedReader fin = new BufferedReader(new FileReader(infile));
				BufferedWriter fout = new BufferedWriter(new FileWriter(outfile));

				String line;
				while ((line = fin.readLine()) != null) {
					for (String key : keys) {
						line = line.replaceAll("%" + key, oClone.getField(key));
					}
					fout.write(line + System.getProperty("line.separator"));
				}
				fout.close();
				fin.close();

			}
			return true;
		} catch (Exception e) {
			_errors.add("ImageEditory.create_uploads: " + e.getMessage());
			noErrors = false;
		}
		return noErrors;
	}

	// -------------------------------
	// Appends a line to the end of a file
	private boolean append_line(GuestFS g, String filepath, String add_line, JSONArray repl) {
		boolean noErrors = true;
		try {
			add_line = nl + JsonUtil.replace(add_line, repl, "search", "replace");
			g.write_append(filepath, add_line.getBytes());
		} catch (Exception e) {
			_errors.add("ImageEditory.append_line: " + e.getMessage());
			noErrors = false;
		}
		return noErrors;
	}

	// -------------------------------
	// Replaces all occurances of a search phrase with a new phrase
	private boolean edit_file(GuestFS g, String filepath, JSONArray edits) {
		boolean noErrors = true;
		try {
			String[] lines = g.read_lines(filepath);
			g.touch(filepath);
			g.truncate(filepath);
			for (String line : lines) {
				try {
					line = JsonUtil.replace(line, edits, "search", "replace") + nl;
					g.write_append(filepath, line.getBytes());
				} catch (Exception e) {
					_errors.add("ImageEditory.edit_file: " + e.getMessage());
					noErrors = false;
				}
			}
		} catch (Exception e) {
			_errors.add("ImageEditory.edit_file: " + e.getMessage());
			noErrors = false;
		}
		return noErrors;
	}

	public List<String> getErrors() {
		List<String> errors = _errors;
		_errors = new ArrayList<String>();
		return errors;
	}
}
