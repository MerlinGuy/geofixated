package org.geof.dpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.geof.db.DBInteract;
import org.geof.db.EntityMap;
import org.geof.db.EntityMapMgr;
import org.geof.dpl.mgr.DomainMgr;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.DomainRequest;
import org.geof.request.MacipRequest;
import org.geof.request.Request;
import org.geof.service.TState;
import org.geof.util.FileUtil;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.libvirt.Domain;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GuestObject implements Runnable {

	public final static String GUEST_ADMIN = "guest_admin";
	public final static String GUEST_PWD = "guest_pwd";
	public final static String NAME = "name";
	public final static String DIRNAME = "dirname";
	public final static String FILENAME = "filename";
	
	public final static String DOMAIN = "domain";
	public final static String DOMAIN_NAME = "domain_name";
	public final static String IPADDRESS = "ipaddress";
	public final static String MACIPID = "macipid";
	public final static String BUILDPLANID = "buildplanid";

	public final static int RUNNING = 1;
	public final static int OFFLINE = 5;

	public final static int START = 0;
	public final static int SHUTDOWN = 1;
	public final static int PAUSE = 2;
	public final static int DB = 0, HOST = 1, DB_HOST = 2;

	private DBInteract _dbi = null;
	private JSONObject _data = null;
	private JSONObject _usr = null;
	
	private DomainMgr _domMgr = null;
	private boolean _initialized = false;

	// Required parameters
	public String guest_admin = null;
	public String guest_pwd = null;

//	public String guestName = null;
	public String dirName = null;
	public String name = null;
	public String fileName = null;	

	public String sourceName = null;
	public String host_img_dir = null;
	public boolean is_tenant = false;

	public String macAddress = null;
	public String ipAddress = null;
	public long macipid = -1;
	public String macDomain = null;
	public int fwport = -1;

	public Buildplan bplan = null;
	
	public String workDirectory = null;
	public String workXmlPath = null;
	public String guestDirectory = null;
	public String xmlPath = null;
	public String templateDirectory = null;

	public Domain sourceDomain = null;

	public ImageFile[] imageFiles = null;
	public List<String> srcImageFiles = null;

	public JSONObject imageChanges = null;
	public HashMap<String, String> _fields = new HashMap<String, String>();

	public int _total_steps = 15;
	public TState _tstate = null;

	// Optional parameters

	public boolean startGuest = false;
	public boolean connectGuest = false;
	public boolean overwriteGuest = false;
	public boolean shutdownSource = false;
	public boolean restartSource = false;
	
	protected Thread _thread = null;
	
	public GuestObject(DomainMgr domMgr, DBInteract dbi, JSONObject data) {
		_dbi = dbi;
		_data = data;
		_domMgr = domMgr;
		_dbi.spawnDbConnection();
	}

	@Override
	public void run() {
		try {
			this.initialize(this._domMgr, this._dbi, this._data);
			this.create();
		} catch (Exception e) {
			GLogger.error("GuestObject.run: " + e.getMessage());
		} finally {
			this._dbi.dispose();
		}
	}

	public void initialize(DomainMgr domMgr, DBInteract dbi, JSONObject data) throws Exception {

		_dbi = dbi;
		_data = data;
		
		JSONObject joDemoBP = DemoBuildplan.getDemoBuildplan(_dbi);
		if (joDemoBP == null) {
			throw new Exception("Missing Demo Buildplan");
		}
		
		_usr = dbi.getUsr();
		
		JSONObject fields = data.getJSONObject(Request.FIELDS);
		startGuest |= fields.optBoolean("start_guest", false);
		if (startGuest) {
			_total_steps++;
		}
		connectGuest |= fields.optBoolean("connect_guest", false);
		if (connectGuest) {
			_total_steps++;
		}
		overwriteGuest = fields.optBoolean("overwrite_guest", false);
		if (overwriteGuest) {
			_total_steps++;
		}
		shutdownSource = fields.optBoolean("shutdown_source", false);
		if (shutdownSource) {
			_total_steps += 2;
		}
		_tstate = new TState(_dbi.getTransaction(), _total_steps);

		long bplanid = joDemoBP.optLong("buildplanid",-1);
		
		if (bplanid != -1) {
			bplan = new Buildplan();
			bplan.initialize(_dbi, bplanid);
			startGuest = bplan.mustConnect();
			connectGuest = startGuest;
			_tstate.log("Retrieved demo buldplan");
		}

		if (this._domMgr == null) {
			this._domMgr = (domMgr == null) ? new DomainMgr(data) : domMgr;
		}
		if (this._domMgr.hasError()) {
			throw new Exception(this._domMgr.getError());
		}
		_tstate.log("DomainMgr active");

		GlobalProp gprop = GlobalProp.instance();
		templateDirectory = gprop.get("basepath") + "templates/";

		// TODO: add this to buildplan
		imageChanges = JsonUtil.parseConfigFile(templateDirectory + "image_changes.cfg");

		dirName = _domMgr.getRandomDomainName();
		fileName = fields.optString("filename", dirName);
		name = fields.optString("name", dirName);

		sourceName = joDemoBP.optString("domainname", null);
		if (sourceName == null) {
			sourceName = gprop.get("guest_domain");
			if (sourceName == null) {
				sourceName = joDemoBP.getString("domainname");
			}
		}

		// Fill out the Macip information
		JSONObject joMacip = MacipRequest.reserve(_dbi, macipid, dirName);
		if (joMacip == null) {
			throw new Exception("Could not reserve macip id:" + macipid + ",domain:" + name);
		}

		macDomain = joMacip.optString("domain", "");
		macAddress = joMacip.getString("macaddress");
		ipAddress = joMacip.getString("ipaddress");
		fwport =  joMacip.optInt("fwport",-1);

		_fields.put("ip", ipAddress);
		_fields.put(DIRNAME, dirName);
		_fields.put(NAME, name);

		_tstate.log("Reserved macip");

		// Get the guest user name and password
		guest_admin = fields.optString(GUEST_ADMIN, gprop.get(GUEST_ADMIN));
		if (guest_admin == null) {
			throw new Exception("Missing guest_admin parameter");
		}

		guest_pwd = fields.optString(GUEST_PWD, gprop.get(GUEST_PWD));
		if (guest_pwd == null) {
			throw new Exception("Missing guest_pwd parameter");
		}

		this.host_img_dir = fields.optString("host_img_dir", null);

		JSONObject joDomain = JsonUtil.getDataWhere(NAME, sourceName);

		EntityMap emap = EntityMapMgr.getEMap(DOMAIN);
		JSONArray domainRows = _dbi.readAsJson(emap, joDomain);
		Domain sourceDomain = null;

		if (domainRows.length() == 0) {
			sourceDomain = domMgr.getDomain(sourceName);
			if (sourceDomain == null) {
				throw new Exception("Source domain: " + sourceName + " doesn't exist");
			}
		}
		_tstate.log("Retrieved source domain");

		// TODO: remove directory in Finally of method
		workDirectory = gprop.get("working_dir", "/tmp/dopple/");
		File fWorkDir = FileUtil.makeDirStruct(workDirectory, dirName);
		workDirectory = fWorkDir.getAbsolutePath();
		workXmlPath = workDirectory + "/" + dirName + ".xml";

		guestDirectory = gprop.get("image_dir");
		guestDirectory = FileUtil.endPath(guestDirectory) + dirName;
		xmlPath = guestDirectory + "/" + dirName + ".xml";

		_tstate.log("Initialization complete");
		_initialized = true;
	}

	/**
	 * 
	 */
	public boolean create() throws Exception {

		GeofInstaller giHostImg = null;
		GeofInstaller giGuest = null;

		if (!this._initialized) {
			throw new Exception("GuestObject has not been initialized");
		}

		try {
			DomainMgr dm = this._domMgr;
			DomainData dData = dm.getDomainData(this.sourceName);
			_tstate.log("DomainData retrieved");

			// Shutdown the source domain if necessary
			if (dm.getDomainOrdinalState(this.sourceName) == RUNNING) {
				if (this.shutdownSource) {
					_tstate.log("Source domain shutdown");
					this.restartSource = dm.shutdown(this.sourceName);
					if (! this.restartSource) {
						throw new Exception("Can't shutdown source domain:" + this.sourceName);
					}
				} else {
					throw new Exception("Source domain is active: " + this.sourceName);
				}
			}

			// Connect to the Host server
			if (dm.isRemoteSvr()) {
				GLogger.debug("Attempting to connect to remote host");
				int max_retries = 12;
				giHostImg = new GeofInstaller(dm.getRemoveSvr(), this.guest_admin, this.guest_pwd );
				giHostImg.connect(max_retries);
				GLogger.debug("... connected to remote host");
			}

			// String host_image_dir = dpl_cfg.optString("host_image_dir","");
			Domain domExisting = dm.getDomain(this.dirName);
			if (domExisting != null) {
				if (this.overwriteGuest) {
					DomainData eData = dm.getDomainData(this.dirName);
					DomainRequest.delete(dm, _dbi, this.dirName, eData.getDomainDirectory());
				} else {
					throw new Exception("Domain: (" + this.dirName + ") already exists");
				}
			}

			GLogger.debug("Creating image directory - " + this.guestDirectory);
			String err = null;
			if (giHostImg != null) {
				giHostImg.create_directory(this.guestDirectory);
			} else {
				File file = new File(this.guestDirectory);
				if (file.exists() && this.overwriteGuest) {
					FileUtil.removeDirectory(file);
				}
				err = FileUtil.makeTargetDir(file);
				if (err != null) {
					throw new Exception(err);
				}
			}
			_tstate.log("Work directories created");

			// TODO: there should be an template xml file that is used when no
			// guest xml (DomainData) is specified. That file should be
			// available from the Domain object
			this.setImages(dData.files);
			String strGuestXml = this.getImageXml(dData.xml_str);
			File file = new File(this.templateDirectory + "guest.xml");
			if (!file.exists()) {
				FileUtils.writeStringToFile(file, strGuestXml);
			}

			if (giHostImg != null) {
				dm.writeNewXml(this.workXmlPath, strGuestXml);
				giHostImg.scp_to(this.workXmlPath, this.xmlPath);
			} else {
				dm.writeNewXml(this.xmlPath, strGuestXml);
			}
			_tstate.log("Guest xml file created");

			// Copying image files from source directory to either
			// working directory or guest image directory
			List<String> work_images = new ArrayList<String>();
			for (ImageFile imgF : this.imageFiles) {
				if (giHostImg != null) {
					if (true /* nDomain.useLocalFiles */) {
						File fileFrom = new File(imgF.source_image);
						File fileHost = new File(imgF.work_image);
						FileUtils.copyFile(fileFrom, fileHost);
					}
					// else {
					// giHostImg.scp_from(imgF.source_image, imgF.work_image);
					// }
					work_images.add(imgF.work_image);
				} else {
					File fileFrom = new File(imgF.source_image);
					File fileHost = new File(imgF.guest_image);
					FileUtils.copyFile(fileFrom, fileHost);
					work_images.add(imgF.guest_image);
				}
			}
			_tstate.log("Image file(s) copied");

			// Restart the shutdown source domain
			if (this.restartSource) {
				if (dm.start(this.sourceName)) {
					_tstate.log("Source domain restarted");
				} else {
					_tstate.log("GuestObject.buildGuest Failed to restart Source domain: " + this.sourceName);
				}
			}

			// Now modify the image file to change things like interfaces file
			ImageEditor imgEditor = new ImageEditor();
			HashMap<String, String> rtn_mod = imgEditor.modify(this);

			if (rtn_mod.containsKey("error")) {
				throw new Exception("ImageEditor.modify : " + rtn_mod.get("error"));
			}
			_tstate.log("Image file(s) modified");

			// The copied images files were edited locally so they need to be
			// scp'd to the host image directory
			if (giHostImg != null) {
				for (ImageFile imgF : this.imageFiles) {
					giHostImg.scp_to(imgF.work_image, imgF.source_image);
				}
			}

			if (!dm.define(this.xmlPath, this.dirName)) {
				throw new Exception("Failed to define new domain: " + this.dirName);
			}
			_tstate.log("Guest defined");

			if (this.startGuest) {
				if (!dm.start(this.dirName)) {
					throw new Exception("Failed to start new domain: " + this.dirName);
				}
				_tstate.log("Guest started");
			}

			if (this.connectGuest) {
				int max_retries = 20;
				giGuest = new GeofInstaller(this);
				if (giGuest.connect(max_retries)) {
					_tstate.log("Guest reachable");
				} else {
					_tstate.log("Failed to connect to new domain");
				}
			}
			
			if ( bplan != null && giGuest != null && giGuest.isConnected()) {
				JSONArray cleanup = new JSONArray();
				giGuest.run_install(bplan, cleanup);
				_tstate.log("Guest install files uploaded");
			}

			MacipRequest.setDomain(_dbi, this.macipid, this.dirName, MacipRequest.IN_USE);
			
			JSONObject fields = _data.getJSONObject(Request.FIELDS);
			fields.put(IPADDRESS, this.ipAddress);
			
			fields.put(NAME, this.name);
			fields.put(DIRNAME, this.dirName);
			// TODO: Replace hard-coded value with created file name
			fields.put(FILENAME, this.guestDirectory + "/" + dirName + "_0.qcow2");

			fields.put("buildplanid", bplan != null ? bplan.id : -999);
			fields.put("email", _usr.optString("email"));
			fields.put("firstname", _usr.optString("firstname"));
			fields.put("lastname", _usr.optString("lastname"));
			fields.put("url", "http://www.geofixated.org:" + fwport);	
			fields.put("login", _usr.optString("login"));			
			fields.put("pwd", org.geof.service.GSession.getRandomPwd(null));			
			
			DomainRequest.register(_dbi, _data, null, dm);
			
			_tstate.log("Guest registered - Complete");

			return true;
		} catch (Exception e) {
			_tstate.logError(e.getMessage());
			this.backout(this.dirName);
			throw new Exception("\nGuestObject.create failed: \n" + e.getMessage());
		} finally {
			this.cleanUp();
			if (giHostImg != null) {
				giHostImg.disconnect();
			}
			if (giGuest != null) {
				giGuest.disconnect();
			}
		}
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public String getField(String key) {
		if (_fields.containsKey(key)) {
			return _fields.get(key);
		} else {
			return "";
		}
	}

	public String[] getFieldKeys() {
		Set<String> keys = _fields.keySet();
		return keys.toArray(new String[keys.size()]);
	}

	public boolean cleanUp() {
		FileUtil.removeDirectory(this.workDirectory);
		return true;
	}

	public boolean backout(String domain) {
		if (domain != null && domain.length() > 0) {
			MacipRequest.clearByDomain(_dbi, domain);
		}
		cleanUp();
		FileUtil.removeDirectory(guestDirectory);
		return true;
	}

	// ---------------------------------
	public void setImages(List<String> srcImageFiles) throws Exception {
		if (srcImageFiles == null) {
			throw new Exception("No files specified for NewDomain.setImages");
		}
		int count = srcImageFiles.size();
		if (srcImageFiles.size() == 0) {
			throw new Exception("No files specified for NewDomain.setImages");
		}

		imageFiles = new ImageFile[count];
		String file = srcImageFiles.get(0);
		String ext = file.substring(file.lastIndexOf("."));
		String srcFile;

		for (int indx = 0; indx < count; indx++) {
			srcFile = srcImageFiles.get(indx);
			String guestImage = guestDirectory + "/" + dirName + "_" + indx + ext;
			// String work_img = null;
			// if (this.work_dir != null) {
			// work_img = work_dir + image_name;
			// }
			// TODO: fix this so that remote hosts can work
			imageFiles[indx] = new ImageFile(srcFile, guestImage, null);
		}
		this.srcImageFiles = srcImageFiles;
	}

	// ---------------------------------
	public HashMap<String, String> getXmlParams() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("/domain/name", this.dirName);
		params.put("//uuid", java.util.UUID.randomUUID().toString());
		params.put("//devices/interface/mac[@address]", this.macAddress);

		String file = "//devices/disk/source[@file=\"";
		String exp;
		int indx = 0;
		for (ImageFile imgF : imageFiles) {
			exp = file + srcImageFiles.get(indx++) + "\"]";
			params.put(exp, imgF.guest_image);
		}

		return params;
	}

	// ---------------------------------
	public String getImageXml(String raw_xml) throws Exception {

		HashMap<String, String> params = getXmlParams();
		if (params == null) {
			return null;
		}
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new ByteArrayInputStream(raw_xml.getBytes("UTF-8")));
		XPath xpath = XPathFactory.newInstance().newXPath();

		String text;
		XPathExpression exp;
		NodeList nl;
		Node node;
		for (String path : params.keySet()) {
			text = params.get(path);
			exp = xpath.compile(path);
			nl = (NodeList) exp.evaluate(doc, XPathConstants.NODESET);
			node = nl.item(0);
			if (node == null) {
				throw new Exception("GuestObject.getImageXml xpath returned null for " + path);
			}

			int indx = path.indexOf('@');
			if (indx > -1) {
				String key = path.substring(indx + 1);
				indx = key.indexOf("=");
				if (indx == -1) {
					indx = key.indexOf("]");
				}
				if (indx > -1) {
					key = key.substring(0, indx);
					NamedNodeMap nnm = node.getAttributes();
					nnm.getNamedItem(key).setNodeValue(text);
				}
			} else {
				node.setTextContent(text);
			}
		}
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		return result.getWriter().toString();
	}

}
