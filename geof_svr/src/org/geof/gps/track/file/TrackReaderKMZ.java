package org.geof.gps.track.file;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.geof.log.GLogger;
import org.geof.util.XMLReader;

public class TrackReaderKMZ extends TrackReaderKML {

	public boolean readTrack(File file) {
		_file = file;
		boolean rtn = false;
		try {
			ZipFile zf = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) entries.nextElement();
//				GLogger.debug("Read " + ze.getName() + "?");
				_reader = XMLReader.create(zf.getInputStream(ze));
				rtn = readTrack();
				zf.close();
				break;
			}			
		} catch (IOException e) {
			GLogger.error(e);
		}
		return rtn;
	}
}
