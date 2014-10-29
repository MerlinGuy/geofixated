package org.geof.dpl;

import org.geof.data.FileInfo;
import org.geof.log.GLogger;
import org.geof.util.FileUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GeofInstaller {

	public int OKAY = 0, STDIN = 1, STDERR = 2;

	JSch _jsch = null;
	Session _session = null;
	String _user = null;
	String _host = null;
	String _pwd = null;
	String _sudo_pwd = null;
	int _port = 22;
	int _timeout = 10000;
	int _maxtries = 3;
	int _sleeptime = 1000;

	boolean _print_cmd = true;
	

	// ------------------------------------------
	public GeofInstaller(String host, String user, String pwd) {
		_host = host;
		_user = user;
		_pwd = pwd;
		_sudo_pwd = pwd;
	}

	public GeofInstaller(GuestObject co) {
		this(co.ipAddress,co.guest_admin,co.guest_pwd);
	}
	
	// ------------------------------------------
	public GeofInstaller(JSONObject config) throws JSONException {
			_host = config.getString("host");
			_user = config.getString("user");
			_pwd = config.getString("pwd");
			_sudo_pwd = config.getString("pwd");
			_port = config.optInt("port", _port);
			_maxtries = config.optInt("maxtries", _maxtries);
			_sleeptime = config.optInt("sleeptime", _sleeptime);
	}
	
	public boolean connect(int maxTries) throws Exception {
		_maxtries = maxTries;
		return connect();
	}
	
	// ------------------------------------------
	public boolean connect() throws Exception {
		boolean connected = false;
		int tries = 0;
		_jsch = new JSch();
		this.disconnect();
		Session session = null; 

		java.util.Properties props = new java.util.Properties();
		props.put("StrictHostKeyChecking", "no");
		while (!connected && tries < _maxtries) {
			try {
				session = _jsch.getSession(_user, _host, _port);
				session.setConfig(props);
				session.setPassword(_pwd);
				session.connect(_timeout);
				connected = true;
			} catch (JSchException jsche) {
				tries += 1;
				Thread.sleep(_sleeptime);
			}
		}
		if (connected) {
			_session = session;
		}
		return connected;
	}

	// ------------------------------------------
	public void disconnect() {
		if (_session != null) {
			_session.disconnect();
			_session = null;
		}
	}
	
	public boolean isConnected() {
		return _session != null && _session.isConnected();
	}

	// ------------------------------------------
	public void run_install(Buildplan bp, JSONArray cleanup_cmds) throws Exception {
		
		ReturnObj ro;

		// ----- INSTALL PACKAGES -----
		for (String pkg : bp.installPkgs) {
			if (! install_dpkg( pkg ) ) {
				throw new Exception("Failed to install pkg: " + pkg);
			}
		}

		// ----- RUN POST INSTALL CHECKS -----
		List<JSONObject> checks = bp.postInstallChecks;
		if (checks != null) {
			GLogger.debug("Running pre-checks...");
			for (JSONObject check : checks) {
				String funcName = check.optString("func", null);
				JSONObject params = check.optJSONObject("params");
				if (funcName.equalsIgnoreCase("is_running")) {
					if (is_running(params)) {
						throw new Exception("Failed checks.. exiting ");
					}
				}
			}
			GLogger.debug("... passed all pre-checks");
		}

		// ----- WGET FILES -----
		for (String url : bp.wgetFiles) {
			if (! wget_file( url ) ) {
				throw new Exception("Failed to install file: " + url);
			}
		}

		// ----- INSTALL FILES FROM SERVER -----
		String remote_dir = "/home/" + _user + "/";
		for (FileInfo fInfo :  bp.installFiles) {
			this.scp_to(fInfo.Fullpath, remote_dir + fInfo.Filename);
		}
		GLogger.debug("... all files rcp'd");

		// ----- ISSUE SETUP COMMANDS -----
		GLogger.debug("Issuing commands...");
		for (String cmd : bp.installCmds) {
			cmd = cmd.trim();
			if (cmd.length() > 0) {
				ro = exec(cmd);
				if (! ro.success) {
					throw new Exception(ro.error);
				}
			}
		}
		GLogger.debug("... all commands issued");

//		if (rcp_files != null) {
//			GLogger.debug("Removing rcp files...");
//			ro = this.delete_files(rcp_files, remote_dir); 
//			if (! ro.success) {
//				GLogger.debug("Error during delete_files - not fatal");
//			}
//			GLogger.debug("... rcp files removed");
//		}

		if (cleanup_cmds != null) {
			GLogger.debug("Running cleanup commands...");
			for (int indx = 0; indx < cleanup_cmds.length(); indx++) {
				String cmd = cleanup_cmds.optString(indx, "");
				ro = exec(cmd);
			}
			GLogger.debug("... cleanup commands complete");
		}
	}

	// ------------------------------------------
	public boolean install_dpkg(String package_name) throws Exception{
		if (package_name == null || package_name.length() == 0 || package_name.startsWith("#", 0)) {
			return true;
		}

		String cmd = "dpkg-query -W -f='${Status}' " + package_name;

		ReturnObj ro = exec(cmd, 60000);
		String rtn = ro.text;
		GLogger.debug("pkg: " + package_name + ", ro:" + ro.success + ", text:" + rtn);
		if (ro.success) {
			if (rtn != null && rtn.indexOf("install ok installed") == -1) {
				// GLogger.debug(stderr.read());
				cmd = "sudo apt-get -y install " + package_name;
				ro = this.exec(cmd);
				GLogger.debug(" --- " + cmd + " : " + rtn);
			}
		}
		return ro.success;
	}

	// ------------------------------------------
	public boolean wget_file(String url) throws Exception{
		if (url == null || url.length() == 0 || url.startsWith("#",0) ) {
			return true;
		}
		String cmd = "wget -N " + url;
		GLogger.error(cmd);
		ReturnObj ro = exec(cmd, 60000);
		return ro.success;
	}

	// ------------------------------------------
	public ReturnObj delete_files(JSONArray files, String remote_dir) {
		try {
			for (int indx = 0; indx < files.length(); indx++) {
				String filename = files.getString(indx);
				String cmd = "rm -f " + remote_dir + filename;
				ReturnObj ro = exec(cmd);
				if (! ro.success) {
					ro.error = "Failed to delete remote file: " + remote_dir + filename;
					return ro;
				}
			}
			return ReturnObj.getOkay();
		} catch (Exception e) {
			return  ReturnObj.getError("Error delete_files: " + e.getMessage());
		}
	}

	// ------------------------------------------
	public void delete_directory(String directory) throws Exception {
		String cmd = "sudo rm -rf " + directory;
		ReturnObj ro = exec(cmd);
		if (! ro.success) {			
			throw new Exception( "Failed to delete dommain directory: " + directory);
		}			
	}

	// ------------------------------------------
	public void create_directory(String remote_dir) throws Exception {
		String cmd = "mkdir " + remote_dir;
		ReturnObj ro = exec(cmd);
		if (! ro.success) {
			throw new Exception ("Failed to make remote directory: " + remote_dir);
		}
	}

	public void copy_file(String from_file, String to_file) throws Exception {
		ReturnObj ro = exec("cp " + from_file + " " + to_file);
		if (! ro.success) {
			throw new Exception ("Failed to copy file from " + from_file + " to " + to_file);
		}
	}

	// ------------------------------------------
	public boolean is_running(JSONObject params) throws Exception {
		String process = params.getString("proc");
		int sleeptime = params.optInt("sleeptime", 2);
		int totaltries = params.optInt("totaltries", 60);
		boolean invert = params.optBoolean("invert",false);

		String cmd = "ps -ef | grep \"" + process + "\" | grep -v \"grep\" |wc -l";
		int tries = 0;

		boolean keep_checking = true;
		while (keep_checking) {
			ReturnObj ro = exec(cmd);
			String stdin = ro.text;
			if ( stdin.length() > 1) {
				stdin = stdin.substring(0,1);
			}
			int proc_count = Integer.parseInt(stdin);
			if (invert) {
				if (proc_count == 0) {
					keep_checking = false;
				}
			} else {
				if (proc_count > 0) {
					keep_checking = false;
				}
			}
			if (keep_checking) {
				tries++;
				if (tries < totaltries) {
					Thread.sleep(sleeptime);
				} else {
					keep_checking = false;
				}
			}
		}			
		return (tries < totaltries);
	}

	
	public ReturnObj exec(String cmd) throws Exception {
		long wait_time = 600000; // 10 minutes default
		return this.exec(cmd, wait_time);
	}
	
	// ------------------------------------------
	public ReturnObj exec(String cmd, long wait_timeout) throws Exception {
		cmd = cmd + "; exit;";
		Channel channel = _session.openChannel("exec");
		boolean isSudo = false;
		int exit_status = 0;
		int cur_wait = 0;
		
		if (cmd.startsWith("sudo ")) {
			cmd = "sudo -S -p '' " + cmd.substring(5);
			isSudo = true;
		}
		GLogger.debug(cmd);
		((ChannelExec) channel).setCommand(cmd);

		InputStream stdin = channel.getInputStream();
		OutputStream out = channel.getOutputStream();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream stderr = new PrintStream(baos);
		((ChannelExec) channel).setErrStream(stderr);

		channel.connect();
		if (isSudo) {
			out.write((_sudo_pwd + "\n").getBytes());
			out.flush();
		}

		boolean complete = false;
		
		while (! complete && (cur_wait < wait_timeout)) {
			exit_status = channel.getExitStatus();
			if (channel.isClosed() || channel.isEOF()) {
//				exit_status = channel.getExitStatus();
				complete = true;
			}
			if (! complete) {
				try {
					Thread.sleep(_sleeptime);
					cur_wait += _sleeptime;
				} catch (Exception ee) {
				}
			}
		}

		channel.disconnect();
		if (complete) {
			String response = FileUtil.getStream(stdin);
			String error = baos.toString("UTF-8");
			return new ReturnObj(true, error, response, exit_status);
		} else {
			throw new Exception("Command timed out - seconds = " + cur_wait );
		}
	}


	// ------------------------------------------
	public void scp_to(String localpath, String remotepath) throws Exception{
		if (_session == null) {
			throw new Exception("GeofInstaller does not have valid session to host" );
		}
		FileInputStream fis = null;
		// exec 'scp -t rfile' remotely
		String command = "scp -t " + remotepath;
		com.jcraft.jsch.Channel channel = _session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();
		channel.connect();

		if (checkAck(in) != 0) {
			throw new Exception("Failed checkAck");
		}

		File _lfile = new File(localpath);

		long filesize = _lfile.length();
		command = "C0644 " + filesize + " ";
		if (localpath.lastIndexOf('/') > 0) {
			command += localpath.substring(localpath.lastIndexOf('/') + 1);
		} else {
			command += localpath;
		}
		command += "\n";
		out.write(command.getBytes());
		out.flush();
		if (checkAck(in) != 0) {
			throw new Exception("Failed checkAck" );
		}

		// send a content of lfile
		fis = new FileInputStream(localpath);
//			long totlen = 0;
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
//			DecimalFormat nf = new DecimalFormat("#.##");
		GLogger.debug("Starting upload at " + df.format(new Date()));
		
		byte[] buf = new byte[4096];
		while (true) {
			int len = fis.read(buf, 0, buf.length);
			if (len <= 0)
				break;
			out.write(buf, 0, len); // out.flush();
//				totlen += len;
		}
		GLogger.debug("... upload complete at " + df.format(new Date()));
		fis.close();
		fis = null;

		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();
		if (checkAck(in) != 0) {
			throw new Exception("Failed checkAck" );
		}
		out.close();
		channel.disconnect();
	}

	public void scp_from(String remotepath, String localpath) throws Exception {
		if (_session == null) {
			throw new Exception("GeofInstaller does not have valid session to host");
		}
		FileOutputStream fos = null;

		String prefix = null;
		if (new File(localpath).isDirectory()) {
			prefix = localpath + File.separator;
		}

		// exec 'scp -f rfile' remotely
		String command = "scp -f " + remotepath;
		Channel channel = _session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();
		channel.connect();

		byte[] buf = new byte[1024];

		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();

		while (true) {
			int c = checkAck(in);
			if (c != 'C') {
				break;
			}

			// read '0644 '
			in.read(buf, 0, 5);

			long filesize = 0L;
			while (true) {
				if (in.read(buf, 0, 1) < 0) {
					// error
					break;
				}
				if (buf[0] == ' ')
					break;
				filesize = filesize * 10L + (long) (buf[0] - '0');
			}

			String file = null;
			for (int i = 0;; i++) {
				in.read(buf, i, 1);
				if (buf[i] == (byte) 0x0a) {
					file = new String(buf, 0, i);
					break;
				}
			}

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			// read a content of lfile
			fos = new FileOutputStream(prefix == null ? localpath : prefix + file);
			int foo;
			while (true) {
				if (buf.length < filesize)
					foo = buf.length;
				else
					foo = (int) filesize;
				foo = in.read(buf, 0, foo);
				if (foo < 0) {
					break;
				}
				fos.write(buf, 0, foo);
				filesize -= foo;
				if (filesize == 0L) {
					break;
				}
			}
			try {
				fos.close();
			} catch (Exception ee) {}
			fos = null;

			if (checkAck(in) != 0) {
				throw new Exception("GeofInstaller.scp_from Failed checkAck");
			}

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
		}

		channel.disconnect();
	}

	// ------------------------------------------
	private static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b < 1) {
			return b;
		}

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
}
