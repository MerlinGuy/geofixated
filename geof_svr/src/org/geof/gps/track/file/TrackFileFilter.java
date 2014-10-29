/**
 * 
 */
package org.geof.gps.track.file;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.geof.util.FileUtil;

/**
 * @author jeff
 * 
 */
public class TrackFileFilter extends FileFilter {

	public final static String[] Track_Extensions = new String[] { "kml", "kmz", "csv", "gpx", "nmea" };
	public final static String COMMA_SPACE = ", ";

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}

		return contains( FileUtil.getExtension(file) );
	}
	
	public boolean contains(String extension) {
		if (extension != null) {
			for (String ext:Track_Extensions) {
				if (ext.compareToIgnoreCase(extension) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		for (String ext : Track_Extensions) {
			sb.append(COMMA_SPACE).append(ext);
		}
		return sb.length() > 0 ? sb.substring(COMMA_SPACE.length()) : sb.toString();
	}

}
