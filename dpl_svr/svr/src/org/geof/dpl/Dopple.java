package org.geof.dpl;

//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;

//import org.apache.commons.io.FileUtils;
import org.geof.log.GLogger;
import org.geof.util.FileUtil;
//import org.geof.util.JsonUtil;
import org.json.JSONObject;
//import org.libvirt.Domain;

public class Dopple {

	public final static int RUNNING = 1;
    public final static int OFFLINE = 5;
	public final static int FROM = 0;
    public final static int TO = 1;

    private String _error = null;
    
	//--------------------------
	public boolean delete_domain(String domain_name, String host, String user, String pwd, String host_img_dir) {
		return false;
//		VirshClone vc = null;
//		GeofInstaller giHost = null;
//		try {
//
//		    String libvirt_conn = "qemu://system";
//		    boolean is_remote_host = host != null;
//		    if (is_remote_host) {
//		        if (user == null) {
//		        	return logError("Missing -user parameter");
//		        }
//		        if (pwd == null) {
//		        	return logError("Missing -pwd parameter");
//		        }	
//		    	libvirt_conn = "qemu+tls://" + host + "/system"; 
//		    }		    
//		    
//		    // Connect to the Host server
//		    vc = new VirshClone();
//		    boolean vc_connected = vc.open(libvirt_conn);
//		    if ( ! vc_connected ){
//		    	return logError("Could not connect to host server");
//		    }
//	
//		    // Get the 'from' domain information (image files, xml, etc)
//	        if (domain_name == null) {
//	        	return logError("Missing -domain_name parameter");
//	        }
//	
//		    //TODO: write return object for calls like this
//		    Domain domain = vc.get_domain(domain_name);
//		    DomainData dData = null;
//		    String domaindir = null;
//		    
//		    if (domain != null) {
//			    // Shutdown the donor domain if necessary
//			    if (vc.get_domain_state(domain_name) == RUNNING) {
//			        GLogger.debug("Shutting down FROM domain - " + domain_name);
//			        if (!vc.shutdown(domain_name)) {
//		                GLogger.debug("Error: could not shutdown target domain -" + domain_name);
//			        }
//			    }
//			    dData = vc.get_image_info(domain_name);
//			    domaindir = dData.getDomainDirectory();
//			    if (! vc.undefine(domain_name)) {
//		            vc.close();
//			    	return logError("Error - failed to remove domain - [" + domain_name + "]");
//		        }
//		        vc.close();
//		    }
//		    if (domaindir == null) {
//		    	if (host_img_dir == null) {
//		    		return logError("Could not find domain and -host_img_dir parameter is null");
//		    	}
//		    	domaindir = host_img_dir + domain_name + "/";
//		    }
//		   
////		    boolean rtn;
//		    if (is_remote_host) {
//
//		    	giHost = new GeofInstaller(host, user, pwd, 12);
//	            
//	            GLogger.debug("Attempting to connect to domain host: " + host);
////	            rtn = giHost.connect().success ;
////	            if (! rtn ) {
////	            	return logError("... connection attempt timed out");
////	            }
//	            GLogger.debug("... connected to " + host);
//	
//	    		rtn = giHost.delete_directory(domaindir).success;
//	    		
//		    } else {
//			    File dir = new File(domaindir);
//			    if (dir.isDirectory()) {
//			        GLogger.debug("Removing existing directory - " + domaindir);
//			        try {
//						FileUtils.deleteDirectory(dir);
//					} catch (IOException e) {
//						return logError("Error - failed to remove domain directory - " + domaindir + e.getMessage());
//					}
//			        if (dir.exists()) {
//			        	return logError("Error - failed to remove domain directory - " + domaindir);
//			        }
//			    }
//			    rtn = true;
//		    }
//	
//		    GLogger.debug("Domain - [" + domain_name + "] removed");
//		    return true;
//	    } catch (Exception e) {
//	    	return logError("--- Dopple Failed ---" + e.getMessage() );
//	    }finally {
//			if ( vc != null) {
//				vc.close();
//			}
//			if ( giHost !=  null) {
//				giHost.disconnect();
//			}
//	    }

	}
		
	//--------------------------
	public boolean delete_domain( JSONObject vargs ) {
		try {
		    String domain_name = vargs.optString("-domain_name",null);	
		    String user = vargs.optString("-user",null);
		    String pwd = vargs.optString("-pwd",null);
	    	String host = vargs.optString("-qemu_svr",null);
	    	String host_img_dir = vargs.optString("-host_img_dir",null);
		    return delete_domain(domain_name, host, user, pwd,host_img_dir);
	    } catch (Exception e) {
	    	return logError("--- Dopple Failed ---" + e.getMessage() );
	    }
	}

    //----------------------------
	public boolean add_domain(JSONObject vargs, JSONObject dpl_cfg, JSONObject chg_cfg ) {

		return false;
//		VirshClone vc = null;
//	    GeofInstaller giHostImg =  null;
//	    GeofInstaller giGuest = null;
//
//		try {
//		    String user = vargs.optString("-user",null);
//		    String pwd = vargs.optString("-pwd",null);
//	        if (user == null) {
//	        	return logError("Missing -user parameter");
//	        }
//	        if (pwd == null) {
//	        	return logError("Missing -pwd parameter");
//	        }
//
//	        boolean start = vargs.optBoolean("-start",false);
//	        String qemu_svr = vargs.optString("-qemu_svr",null);
//		    boolean is_remote_host = qemu_svr != null;
//		    boolean connect = vargs.optBoolean("-connect", is_remote_host);
//		    boolean shutdown = vargs.optBoolean("-shut",false);
//
//	    	String wdir = dpl_cfg.optString("workingdir","./");
//	    	
//		    String mac_cfg_path = wdir + "mac.cfg";
//		    JSONObject mac_addrs= null;
//		    if ((new File(mac_cfg_path)).exists()) {
//		        try {
//		            mac_addrs = JsonUtil.getJObject(mac_cfg_path, null,null);
//		        } catch (Exception e) {
//		        	return logError("Malformed mac / ip file - " + mac_cfg_path);
//		        }
//		    } else {
//		    	return logError("Missing mac / ip file - " + mac_cfg_path);
//		    }
//		    boolean restart = false;          
//		    
//		    String ip = vargs.optString ("-ip", null);
//		    if (! mac_addrs.has(ip) ){
//		    	return logError("Mac address tail .get(" + ip + ") not in range 200:209");
//		    }
//		    
//		    ReturnObj ro;
//		    if (vargs.optBoolean("-build", false)) {
//		        String build_dir = dpl_cfg.optString("build_dir",null);
//		        String ant_build = dpl_cfg.optString("ant_build",null);
//		        if (build_dir != null) {
//		            String ant_cmd = dpl_cfg.optString("ant_cmd", "svr_create_tar");
//		            String local_dir = dpl_cfg.optString("local_dir","");
//		            String cmd = " ant -buildfile " + ant_build 
//		            		+ " -Darg0=" + build_dir
//		            		+ " -Darg1=" + user 
//		            		+ " -Darg2=" + pwd 
//		            		+ " -Darg3=" + local_dir
//		            		+ " " + ant_cmd;
//		            
//		            GLogger.debug(cmd);
//		            ro = exec(cmd);
//		            if (ro.exit_code != 0) {
//		            	return logError("Error- Ant build failed " + ro.error);
//		            }
//		        } else {
//		        	return logError("build_dir"  + " is missing from dopple.cfg file");
//		        }
//		    }
//		    
//		    //TODO: change this to something like software install file
//		    String install_file = vargs.optString("-install_file",null);
//		    if (install_file != null) {
//		    	install_file = wdir + install_file;
//		        if (! (new File(install_file)).exists()) {
//		        	return logError("Missing install file - " + install_file);
//		        }
//
//		        start = true;
//		        connect = true;
//		                
//		    } else if ( connect ) {
//		        start = true;
//		    }
//		    
//		    String libvirt_conn = "qemu://system";
//		    if (is_remote_host) {
//		    	libvirt_conn = vargs.optString("-qemu","qemu+tls") + "://" + qemu_svr + "/system"; 
//		    }		    
//		    
//		    // Connect to the Host server
//		    GLogger.debug("Connecting to virsh server");
//		    vc = new VirshClone();
//		    boolean vc_connected = vc.open(libvirt_conn);
//		    if ( ! vc_connected ){
//		    	return logError("Could not connect to host server");
//		    }
//		    GLogger.debug("... connected server");
//
//		    // Get the 'from' domain information (image files, xml, etc)
//		    String from_domain = vargs.optString("-from",null);		    
//		    //TODO: write return object for calls like this
//		    DomainData dData = vc.get_image_info(from_domain);
//		    
//		    // Shutdown the donor domain if necessary
//		    if (vc.get_domain_state(from_domain) == RUNNING) {
//		        GLogger.debug("Shutting down source domain - " + from_domain);
//		        if (shutdown) {
//		            restart = vc.shutdown(from_domain);
//		            if (! restart) {
//		                GLogger.debug("Error: could ! shutdown source domain -" + from_domain);
//		            }
//		        } else {
//		        	return logError("Error: source domain == active - " + from_domain);
//		        }
//		    }		    
//		    
//		    boolean rtn = false;
//		    if (is_remote_host){
//		        GLogger.debug("Attempting to connect to remote host");
//		        int max_retries = 12;
//		        giHostImg = new GeofInstaller( qemu_svr, user, pwd, max_retries );		        
////		        if (! giHostImg.connect().success ) {
////		        	return logError("Connection to remote host timed out");
////		        }
//		        GLogger.debug("... connected to remote host");
//		    }
//		        
//		    String host_image_dir = dpl_cfg.optString("host_image_dir","");
//		    String new_domain = vargs.optString("-domain_name");    
//		    
//		    Domain domExisting = vc.get_domain(new_domain);
//		    if ( domExisting != null ) {
//		    	GLogger.debug("New domain exists on host: " + new_domain);
//		        if (vargs.optBoolean("-remove",false) ) {
//		        	GLogger.debug("Removing existing domain");
//		        	DomainData eData = vc.get_image_info(new_domain);
//		            if (! delete_domain(new_domain, qemu_svr, user, pwd, eData.getDomainDirectory())) {
//		                GLogger.debug("Error. Failed to remove existing new domain - " + new_domain);
//		            }
//		        	GLogger.debug("... domain removed");
//		        } else {
//		        	return logError("Error. Domain: .get(" + new_domain + ") already exists"); 
//		        }
//		    }
//		         
//		    String work_dir = dpl_cfg.optString("work_dir","/tmp");
//		    List<String> local_images = JsonUtil.toList(vargs.optJSONArray("-local_images"));
//		    
//		    NewDomain nDomain = new NewDomain(new_domain, host_image_dir, work_dir);
//		    
//		    if (! nDomain.setImages(dData, local_images)) {
//		    	return logError(nDomain.error); 
//		    }
//		    
//		    String domainDir = nDomain.host_domain_dir;
//
//		    GLogger.debug("Creating image directory - " + domainDir);
//		    String err = null;
//		    if (giHostImg != null) {
//		    	giHostImg.create_directory(domainDir);
//		    } else {
//		    	err = FileUtil.makeTargetDir(domainDir);		    
//		    }
//		    
//		    if (err != null) {
//		    	return logError(err);
//		    }
//		    GLogger.debug("... image directory created.");
//
//		    String mac_address = mac_addrs.optString(ip);		    
//		    String new_xml = nDomain.getImageXml(dData.xml_str, mac_address);
//		    if (new_xml == null) {
//		    	return logError(nDomain.error);
//		    }
//		    
//		    String new_xml_path = nDomain.xml_path;
//		    GLogger.debug("Creating image xml config file - " + new_xml_path);
//		    rtn = false;
//
////		    if (giHostImg != null) {
////		    	String tmp_path =  work_dir + "/" + new_domain + ".xml";
////		    	rtn = vc.write_new_xml(tmp_path, new_xml);
////		    	rtn = giHostImg.scp_to(tmp_path , new_xml_path).success;
////		    } else {
////		    	rtn = vc.write_new_xml(new_xml_path, new_xml);
////		    }
////		    if (! rtn) {
////		    	return logError("Unknown error while creating new domain xml file");
////		    }
//
//		    List<String> work_images = new ArrayList<String>();
////		    for (ImageFile imgF : nDomain.imgFiles) {
////		        GLogger.debug("Copying image files - " + imgF.from_image);
////		        if (giHostImg != null) {
////		        	if (nDomain.useLocalFiles) {
////			        	File fileFrom = new File(imgF.from_image);
////			        	File fileHost = new File( imgF.work_image);
////				        FileUtils.copyFile(fileFrom, fileHost);
////		        	}else {
////			        	ro = giHostImg.scp_from(imgF.from_image, imgF.work_image);
////		        	}
////		        	work_images.add(imgF.work_image);
////		        } else {
////		        	File fileFrom = new File(imgF.from_image);
////		        	File fileHost = new File( imgF.host_image);
////			        FileUtils.copyFile(fileFrom, fileHost);
////		        	work_images.add(imgF.host_image);
////		        }
////		    }
//		        
//		    // Restart the shutdown source domain
//		    if (restart) {
//		        if (! vc.start(from_domain)) {
//		            GLogger.debug("Failed to restart from  domain - " + from_domain);
//		        }
//		    }
//
//		    ip = dpl_cfg.optString("baseip","192.168.1.") + ip;
//		    vargs.put("-ip", ip);
//		    
//		    JSONObject mod_cfg = new JSONObject(
//		    		"{'template_dir':'" + dpl_cfg.optString("template_dir") + "'}"
//		    	);
//		    mod_cfg.put("imgfiles",work_images);
//		    
//		    GLogger.debug("Modifying files in new image");
//		    ImageEditor imgEditor = new ImageEditor();
////		    HashMap<String,String> rtn_mod = imgEditor.modify(mod_cfg, chg_cfg, vargs);
////		    
////		    if (rtn_mod.containsKey("error")){
////		    	return logError("GuestFileModifier.modify : " + rtn_mod.get("error"));
////		    }
//		    GLogger.debug("... modifications complete on new image");
//		    
//		    // The copied images files were editted locally so they need to be 
//		    // scp'd to the host image directory
////		    if (giHostImg != null) {
////			    GLogger.debug("Copying new image to remotehost");
////		    	for (ImageFile imgF : nDomain.imgFiles) {
////			    	if (! giHostImg.scp_to(imgF.work_image , imgF.host_image).success){
////			    		return logError("... failed to copy image " + imgF.work_image + " to " + imgF.host_image);
////			    	}
////		    	}
////	        }
//
//		    String cmd = "sudo chmod -R 777 " + nDomain.host_domain_dir;		    
//	        if (giHostImg != null) {
//	        	ro = giHostImg.exec(cmd);	        	
//	        } else {
//	        	ro = exec(cmd);
//	        }
//	        if (giHostImg != null) {
//	        	giHostImg.disconnect();
//	        	giHostImg = null;
//	        	GLogger.debug("Disconnecting from remote host: " + qemu_svr);
//	        }
//	                    
//            if (ro.exit_code > 0) {
//            	return logError("Error " + cmd + "  returned: " + ro.error);
//            }
//
//		    GLogger.debug("Defining new domain" );
//		    if (! vc.define(new_xml, new_domain)) {
//		        GLogger.debug("Error. Failed to define new domain - " + new_domain);
//		    }
//		        
//		    if (start) {   
//		        GLogger.debug("Starting new domain");
//		        if (! vc.start(new_domain)) {
//		        	return logError("Error. Failed to start new domain - " + new_domain);
//		        }
//		    }		        
//		    
////		    if (connect) {
////		        GLogger.debug("Connecting to new domain - " + ip);
////		    	int max_retries = 20;
////		    	giGuest = new GeofInstaller( ip, user, pwd, max_retries );
////		    	if (! giGuest.connect().success) {
////		    		return logError("Error. Failed to connect to new domain - " + new_domain);
////		    	}
////		        GLogger.debug("... connected to new domain");
////		    }
//		        
////		    if (install_file != null) {
////		        GLogger.debug("Installing files on new domain");
////		        JSONArray cleanup = new JSONArray();    
////		        if (! giGuest.run_install(install_file, cleanup).success) {
////		        	return logError("Error. Failed to complete software install on new domain");
////		        }
////		        GLogger.debug("... files installed");
////		    }
//		    
//		    GLogger.debug("--- Dopple complete ---" );
//		    return true;
//	    } catch (Exception e) {
//	    	return logError("--- Dopple Failed ---" + e.getMessage() );
//	    } finally {
//			if ( vc != null) {
//				vc.close();
//			}
//			if ( giHostImg !=  null) {
//				giHostImg.disconnect();
//			}
//			if ( giGuest != null) {
//				giGuest.disconnect();
//			}
//
//	    }
    }
	    
	public ReturnObj exec(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			int exit_code = p.waitFor();
	    	String stdout = FileUtil.getStream(p.getInputStream());
	    	String stderr = FileUtil.getStream(p.getErrorStream());
	        return new ReturnObj(exit_code == 0, stderr, stdout, exit_code);
		} catch (Exception e) {
			return ReturnObj.getError(e.getMessage());
		}
	}
	
	private boolean logError(String error) {
		_error = error;
		GLogger.error(error);
		return false;
	}
	
	public String getError() {
		String error = _error;
		_error = null;
		return error;
	}
	
	//-----------------------------------------------
//	public static void main(String [] args)	{
//		try {
//			
//			
//		    JSONObject vargs = new JSONObject();
//
//		    String key = null;
//		    for (String arg : args) {
//		    	if (arg.startsWith("-")){
//		    		key = arg;
//		    	} else {
//		    		if (key == null) {
//		    			println("Error: invalid commandline arguements");
//		    			System.exit(1);
//		    		}
//		    		vargs.put(key,arg);
//		    		key = null;
//		    	}
//		    }
//		    
//		    JSONObject config = JsonUtil.getJObject("dopple.cfg",null,null);
//
//		    if (vargs.has("-add")) {
//				JSONArray add_req = new JSONArray("["
//			            +"{'key':'-from', 'help':'(source domain) required'},"
//			            +"{'key':'-add', 'help':'(new domain name) required'},"
//			            +"{'key':'-ip', 'help':'(ip address) required'},"
//			            +"]");
//				
//		        boolean hasArgError = false;
//		        
//		        for (int indx=0; indx<add_req.length(); indx++) {
//		        	JSONObject req = add_req.getJSONObject(indx); 
//			        key = req.optString("key");
//			        if (!vargs.has(key)) {
//		                println(key + " : " + req.optString("help"));
//		                hasArgError = false;
//			        } else if ( vargs.get(key) == null) {
//		                println("Value for " + key + " missing");
//		                hasArgError = false;
//			        }
//		        }
//		    
//		        if (hasArgError) {
//		        	System.exit(0);
//		        } else {
//		            (new Dopple()).add_domain(vargs, config);
//		        }
//		    }
//		    else if (vargs.has("-remove")) {
//		        
//		    	String domainname =vargs.optString("-remove"); 
//		        if (domainname == null){
//		            println(" Error -remove command requires a domain name.");
//		            System.exit(0);
//		        } else {
//		        	(new Dopple()).remove_domain(domainname, config);
//		        }
//		    } else {
//		        println(" Error -add <domain> or -remove <domain> required.");
//		    }
//		    println(vargs);			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	    

}
