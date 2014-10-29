package org.geof.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.geof.prop.GlobalProp;
import org.geof.log.GLogger;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileChangeWatcher implements Runnable {

	private String _directory = null;
	private String _fullpath = null;
	private File _file = null;
	private GlobalProp _globalProp = null;
	
	private FileChangeWatcher _instance = null;
	private WatchService _watchService = null;
	
	//----------------------------------------------------
	public FileChangeWatcher(GlobalProp globalProp){	
		_globalProp = globalProp;
	}
	
	//----------------------------------------------------
	public boolean initialize(String fullpath) {
		_file = new File(fullpath);
		if (! _file.exists()) {
			return false;
		}
		int indx = fullpath.lastIndexOf("/");
		_directory = fullpath.substring(0,indx);	
		_fullpath = fullpath;
		return true;
	}
	
	public String getPath() {
		return _fullpath;
	}
	
	public FileChangeWatcher get() {
		return _instance;
	}
	
	@Override
	public void run() {
		try {
			final Path path = Paths.get(_directory);
			String filename = _file.getName();
			_watchService = FileSystems.getDefault().newWatchService();
			WatchKey watchKey = path.register(_watchService, ENTRY_MODIFY);
			while (true) {
				watchKey = _watchService.take();
			    for (WatchEvent<?> event : watchKey.pollEvents()) {
			        final Path changed = (Path) event.context();
			        if (changed.endsWith(filename)) {
			        	_globalProp.reload(null);
			        }
			    }
			    // reset the key
			    boolean valid = watchKey.reset();
			    if (!valid) {
			        GLogger.error("Key has been unregistered");
			    }
			}
		} catch (IOException e) {
			GLogger.error(e);
		} catch (InterruptedException e) {
			GLogger.error(e);
		}		
	}

	public void dispose() {
		if (_watchService != null) {
			try {
				_watchService.close();
			} catch (IOException e) {
				GLogger.error(e);
			}
		}
	}
}
