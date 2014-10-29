package org.geof.dpl.mgr;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.RandomStringUtils;
import org.geof.dpl.DomainData;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.DBRequest;
import org.geof.util.FileUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class DomainMgr {

	public final static String BLOCKED = "VIR_DOMAIN_BLOCKED";
	public final static String CRASHED = "VIR_DOMAIN_CRASHED"; 
    public final static String NOSTATE = "VIR_DOMAIN_NOSTATE";
    public final static String PAUSED = "VIR_DOMAIN_PAUSED"; 
    public final static String RUNNING = "VIR_DOMAIN_RUNNING";
    public final static String SHUTDOWN = "VIR_DOMAIN_SHUTDOWN";
    public final static String SHUTOFF = "VIR_DOMAIN_SHUTOFF";
    
	public final static String DEFAULT_CONN = "qemu:///system";
	
	public final static String STATUS = "status";
	private final static int DOMAIN_NAME_LENGTH = 8;
	
	private String _conn_str = null;
	private Connect _conn;
	private String _remote_svr = null;	
	private String _error = null;
	
	public DomainMgr ( ) {	
		this("");
	}
	
	public DomainMgr (String conn_string) {
		connect(conn_string);
	}		
	
	public DomainMgr (JSONObject data) {
		connect(data.optString("qemu_svr", null));
	}		
	
	public boolean connect(String conn_string) {
        try {
			_conn_str = conn_string;
			if (_conn_str == null || _conn_str.length() == 0) {
				_conn_str = GlobalProp.instance().get("libvirt_conn", DEFAULT_CONN);
			}
			if (_conn_str.indexOf("///") == -1) {
				int indx = _conn_str.indexOf("//");
				int next = _conn_str.indexOf("/", indx + 2);
				if (next != -1) {
					_remote_svr = _conn_str.substring(indx, next); 
				}
			}
//			if (is_remote_host) {
//				libvirt_conn = _data.optString("qemu", "qemu+tls") + "://"
//						+ qemu_svr + "/system";
//			}

			if (_conn != null) {
				this.dispose();
			}
			_conn = new Connect(_conn_str);
			return true;
		} catch (LibvirtException e) {
			setError( e.getMessage());
			_conn = null;
		}
        return false;
	}
	
	public boolean isRemoteSvr() {
		return _remote_svr != null;
	}
	
	public String getRemoveSvr() {
		return _remote_svr;
	}
	/**
	 * 
	 * @return
	 */
	public int dispose() {
		int rtn = 0;
		if (_conn != null) {
			try {
				rtn = _conn.close();
			} catch (LibvirtException e) {
				rtn = -1;
				setError(e.getMessage());
			}
			_conn = null;
		}
		return rtn;
	}
	
	/**
	 * 
	 * @param xmlpath
	 * @param name
	 * @return
	 * @throws LibvirtException 
	 * @throws FileNotFoundException 
	 */
	public boolean define(String xmlpath, String name) throws LibvirtException, FileNotFoundException {
		String xmlDesc = FileUtil.parseFile(xmlpath);
        _conn.domainDefineXML(xmlDesc);
        return (this.getDomain(name) != null);
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean undefine(String name) {
	    try {
	        Domain domain = this.getDomain(name);
	        if (domain == null){
	            return true;
	        }
	        else {
	            if (domain.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
	                if (! this.shutdown(name) ) {
	                    return false;
	                }
	            }
	                
	            domain.undefine();
	            domain = this.getDomain(name);
	            if (domain == null) {
	                return true;
	            }
	        }
	    } catch(Exception e) {
	       setError( "Could not undefine domain "+ name + " due to: " +  e.getMessage());
	    }
        return false;
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean shutdown(String name) {
	    try {
	        Domain domain = this.getDomain(name);
	        if (domain == null) {
	            return true;
	        }

	        if (domain.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) {
	            return true ;
	        }
	        
	        domain.destroy();
	        domain = this.getDomain(name);
	        return (domain.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF);
	            
	    } catch(Exception e) {
	    	setError( e.getMessage());
	        return false;
	    }
	}
		
	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean start(String name) {
	    try {
	        Domain domain = this.getDomain(name);         
	        domain.create();
	        return (domain.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING);
	    } catch(Exception e){
	    	setError( e.getMessage());
	        return false;
	    }
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean pause(String name) {
	    try {
	        Domain domain = this.getDomain(name);         
	        domain.suspend();
	        return (domain.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED);
	    } catch(Exception e){
	    	setError( e.getMessage());
	        return false;
	    }
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 * @throws LibvirtException
	 */
	public int getDomainOrdinalState(String name) throws LibvirtException {
	    Domain domain = this.getDomain(name);
	    if (domain == null) {
	        return -1;
	    } else {
	        return domain.getInfo().state.ordinal();
	    }
	}
	
	public DomainInfo.DomainState getDomainState(String name) throws LibvirtException {
	    Domain domain = this.getDomain(name);
	    if (domain == null) {
	        return null;
	    } else {
	        return domain.getInfo().state;
	    }
	}
	
	/**
	 * 
	 * @param dirname
	 * @return
	 */
	public Domain getDomain(String dirname) {
		try {
			return _conn.domainLookupByName(dirname)  ;    
		} catch (Exception e) {
			return null;
		}
	}
	    
	public List<Domain> getDomains() throws Exception {
		
		List<Domain> domains = new ArrayList<Domain>();
		for ( int id : _conn.listDomains()) {
			domains.add( _conn.domainLookupByID(id));
		}
	    return domains;      
	}
	
	public JSONArray getDomainsAsJSONArray() throws Exception {
		
		JSONArray domains = new JSONArray();
		for ( int id : _conn.listDomains()) {
			domains.put( getDomainInfo(_conn.domainLookupByID(id)));
		}
		for ( String name : _conn.listDefinedDomains()) {
			 // Cindy
			domains.put( getDomainInfo(_conn.domainLookupByName(name)));
		}
	    return domains;      
	}
	
	public HashMap<String,Domain> getAllDomains() throws Exception {
		
		HashMap<String,Domain> domains = new HashMap<String,Domain>();
		Domain domain;
		for ( int id : _conn.listDomains()) {
			domain = _conn.domainLookupByID(id);
			domains.put( domain.getName(), domain);
		}
		for ( String name : _conn.listDefinedDomains()) {
			domain = _conn.domainLookupByName(name);
			domains.put( domain.getName(), domain);
		}
	    return domains;      
	}
	
	public static JSONObject getDomainInfo(Domain domain) {
		try {
			JSONObject jDom = new JSONObject();
			jDom.put(DBRequest.ID, domain.getID());
			jDom.put(DBRequest.NAME, domain.getName());
			//CINDY
			jDom.put(DBRequest.DIRNAME, domain.getName());
			jDom.put(STATUS, domain.getInfo().state.ordinal());
			return jDom;
		} catch (Exception e) {
			GLogger.error(e.getMessage());
			return null;
		}		
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @throws LibvirtException
	 */
	public Domain getDomainById(int id) throws LibvirtException {
		return _conn.domainLookupByID(id);
	}

	/**
	 * 
	 * @param state
	 * @return
	 * @throws Exception
	 */
	public Integer[] getDomainIdsByState(String state) throws Exception {
		
		List<Domain> domains = getDomainsByState( state );
		int count = domains.size();
		Integer[] ids = new Integer[count];
		
		for ( int indx=0; indx < count; indx++) {
			ids[indx] = domains.get(indx).getID();
		}
	    return ids;      
	}

	/**
	 * 
	 * @param state
	 * @return
	 * @throws Exception
	 */
	public List<Domain> getDomainsByState(String state) throws Exception {
		
		List<Domain> domains = new ArrayList<Domain>();
		for ( int id : _conn.listDomains()) {
			Domain domain = _conn.domainLookupByID(id);
			DomainInfo di = domain.getInfo();
			if (state == null || di.state.toString().compareTo(state) == -1) {
				domains.add(domain);
			}
		}
	    return domains;      
	}
	
	/**
	 * 
	 * @param state
	 * @return
	 * @throws Exception
	 */
	public JSONArray getDomainsByState(int stateid) throws Exception {
		
		JSONArray domains = new JSONArray();
		for ( int id : _conn.listDomains()) {
			Domain domain = _conn.domainLookupByID(id);
			DomainInfo di = domain.getInfo();
			if (di.state.ordinal() == stateid) {
				domains.put(domain);
			}
		}
	    return domains;      
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 * @throws LibvirtException 
	 */
	public DomainData getDomainData(String name) throws LibvirtException {
		return getDomainData(this.getDomain(name));
	}
			
	/**
	 * 
	 * @param name
	 * @return
	 * @throws LibvirtException 
	 */
	public DomainData getDomainData(Domain domain) throws LibvirtException {
		
		DomainData dd = new DomainData(domain.getName());
		try {			
			dd.domain = domain;
			String raw_xml = domain.getXMLDesc(0);
			dd.setXmlStr( raw_xml);
			XPath xpath = XPathFactory.newInstance().newXPath();
			InputSource iSrc = new InputSource(new StringReader(raw_xml));

			String exp = "//devices/disk/source";
			iSrc = new InputSource(new StringReader(raw_xml));
			NodeList nodes = (NodeList) xpath.evaluate(exp, iSrc, XPathConstants.NODESET);
			for (int indx=0;indx<nodes.getLength();indx++) {
				Node currentItem = nodes.item(indx);
				Node filename = currentItem.getAttributes().getNamedItem("file");
			    dd.addFile( filename.getNodeValue() );
			}
			
		} catch (Exception e) {
			try {
				setError( e.getMessage());
			} catch (Exception eo){}
		}
		return dd;
	}
			
	/**
	 * 
	 * @param filename
	 * @param contents
	 * @return
	 */
	public void writeNewXml(String filename, String contents) throws Exception{
    	BufferedWriter out = new BufferedWriter(new FileWriter(filename));
	    out.write(contents);
        out.close();
	}   
	           
	/**
	 * 
	 * @return
	 */
	public String randomMAC() {
	    Random rand = new Random();
	    String mac_base = GlobalProp.instance().get("mac_base");
	    return mac_base  + ":" 
	    	+ Integer.toHexString(rand.nextInt(0xFF))
	      	+ ":" + Integer.toHexString(rand.nextInt(0xFF))
	      	+ ":" + Integer.toHexString(rand.nextInt(0xFF)); 
	}	
	
	public String getRandomDomainName() {
		String name = null;
		do {
			name = RandomStringUtils.randomAlphabetic(DOMAIN_NAME_LENGTH); 			
		} while (getDomain(name) != null);
		return name;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getError() {
		String rtn = "";
		if (_error != null) {
			rtn = _error;
			_error = null;
		}
		return rtn;
				
	}

	/**
	 * 
	 * @return
	 */
	public boolean hasError () {
		return _error != null;
	}
	
	private void setError (String error) {
		GLogger.debug(error);
		_error = error;
	}
	
}
