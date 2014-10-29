package org.geof.dpl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.EncoderException;
import org.geof.util.ConvertUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libvirt.Domain;

public class DomainData {
	public String name = null;
	public List<String> files = new ArrayList<String>();
	public String startDate = null;
	public String endDate = null;
	public String xml_str = null;
	public Domain domain;
	public Long domain_id = null;
	public String dirname = null;
	public String filename = null;
	
	public DomainData(String name) {
		this.name = name;
	}
	
	public void addFile(String filepath) {
		files.add(filepath);
	}
	
	public void setXmlStr(String xmlstr) {
		xml_str = xmlstr;
	}
	
	public String getDomainDirectory() {
		if (files == null || files.size() < 1) {
			return null;
		}
		String file = files.get(0);
		String dir = name + "/";
		int indx = file.indexOf(dir);
		if (indx == -1) {
			return null;
		}
		return file.substring(0, indx + dir.length());
	}
	
	public JSONObject toJSON() throws JSONException, EncoderException {
		JSONObject jo = new JSONObject();
		jo.put("name", this.name);
		JSONArray jafiles = new JSONArray();
		for (int indx = 0; indx < this.files.size(); indx++) {
			jafiles.put(this.files.get(indx));
		}
		jo.put("xml_str", ConvertUtil.encode(this.xml_str));
		jo.put("files", jafiles);
		return jo;
	}
}
