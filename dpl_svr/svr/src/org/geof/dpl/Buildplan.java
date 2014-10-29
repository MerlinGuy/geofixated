package org.geof.dpl;

import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.geof.data.FileInfo;
import org.geof.db.DBInteract;
import org.geof.request.BuildplanRequest;
import org.geof.store.StorageLocation;
import org.geof.store.StorageMgr;
import org.geof.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;


public class Buildplan {

	public static final String IMGCFG = "imgcfg";
	public static final String PACKAGES = "pkgs";
	public static final String CHECKS = "checks";
	public static final String WGETS = "wgets";
	public static final String COMMANDS = "cmds";
	public static final String RCPS = "rcps";
	public static final String STARTDATE = "startdate";
	public static final String ENDDATE = "enddate";
	public static final String STORAGELOCID = "storagelocid";
	
	public long id = -1;
	
	public List<FileInfo> imageFiles = null;
	
	public String[] installPkgs = null;
	public List<FileInfo> installFiles = null;
	public String[] wgetFiles = null;
	public List<JSONObject> postInstallChecks = null;
	public String[] installCmds = null;
	public StorageLocation storageLoc = null;
	
	public boolean mustConnect() {
		return (installPkgs.length > 0
				|| installFiles.size() > 0
				|| wgetFiles.length > 0
				|| installCmds.length > 0
				|| postInstallChecks.size() > 0);
	}
	
	public void initialize(DBInteract dbi, long buildplanId ) throws Exception {
		this.id = buildplanId;
		JSONObject data = JsonUtil.getDataWhere(id);
		JSONArray jaBP = BuildplanRequest.getBuildPlan(dbi, data);
		if (jaBP.length() != 1) {
			throw new Exception("Buildplan not found - id: " + buildplanId);
		}
		JSONObject joBP = jaBP.getJSONObject(0);
		
		long storagelocid = joBP.optLong(STORAGELOCID, -1);
		this.storageLoc = StorageMgr.instance().getById(storagelocid);
		
		String strImgCfg = new String((new Base64()).decode(joBP.optString(IMGCFG, "{}")));
		JSONObject imgcfg = new JSONObject(strImgCfg);
		
		String pkgs = imgcfg.optString(PACKAGES);
		installPkgs = (pkgs.length() > 0) ? pkgs.split(",") : new String[0];

		String wgets = imgcfg.optString(WGETS);
		wgetFiles = (wgets.length() > 0) ? wgets.split(",") : new String[0];
					
		JSONArray jaChecks = imgcfg.optJSONArray(CHECKS);
		this.postInstallChecks = new ArrayList<JSONObject>();
		if (jaChecks != null) {
			for (int indx=0;indx<jaChecks.length();indx++) {
				this.postInstallChecks.add(jaChecks.getJSONObject(indx));
			}
		}
			
		String cmds = imgcfg.optString(COMMANDS);
		this.installCmds = (cmds.length() > 0) ? cmds.split(",") : new String[0];
		this.imageFiles= BuildplanRequest.getImageFiles( dbi, buildplanId);
		this.installFiles = BuildplanRequest.getInstallFiles( dbi, buildplanId);
	}
		
	public void initialize(DBInteract dbi, JSONObject joDemoBP) throws Exception {
		this.id = joDemoBP.getLong("buildplanid");
		JSONObject data = JsonUtil.getDataWhere(id);
		JSONArray jaBP = BuildplanRequest.getBuildPlan(dbi, data);
		if (jaBP.length() != 1) {
			throw new Exception("Buildplan not found - id: " + this.id);
		}
		JSONObject joBP = jaBP.getJSONObject(0);
		
		long storagelocid = joBP.optLong(STORAGELOCID, -1);
		this.storageLoc = StorageMgr.instance().getById(storagelocid);
		
		Date startdate = (Date) joDemoBP.opt(STARTDATE);
		if (startdate != null) {
			joBP.put(STARTDATE, startdate);
		}
		Date enddate = (Date) joDemoBP.opt(ENDDATE);
		if (enddate != null) {
			joBP.put(ENDDATE, enddate);
		}
		
		String strImgCfg = new String((new Base64()).decode(joBP.optString(IMGCFG, "{}")));
		JSONObject imgcfg = new JSONObject(strImgCfg);
		
		String pkgs = imgcfg.optString(PACKAGES);
		installPkgs = (pkgs.length() > 0) ? pkgs.split(",") : new String[0];
		
		String wgets = imgcfg.optString(WGETS);
		wgetFiles = (wgets.length() > 0) ? wgets.split(",") : new String[0];
					
		JSONArray jaChecks = imgcfg.optJSONArray(CHECKS);
		this.postInstallChecks = new ArrayList<JSONObject>();
		if (jaChecks != null) {
			for (int indx=0;indx<jaChecks.length();indx++) {
				this.postInstallChecks.add(jaChecks.getJSONObject(indx));
			}
		}
			
		String cmds = imgcfg.optString(COMMANDS);
		this.installCmds = (cmds.length() > 0) ? cmds.split(",") : new String[0];			
		this.imageFiles= BuildplanRequest.getImageFiles( dbi, this.id);		
		this.installFiles = BuildplanRequest.getInstallFiles( dbi, this.id);
	}
}
