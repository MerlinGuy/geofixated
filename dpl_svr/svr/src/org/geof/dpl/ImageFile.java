package org.geof.dpl;

public class ImageFile {
	
	public String source_image = null; // name of the actual source image file
	public String guest_image = null; // name of the final new image file on host
	public String work_image = null; // path to image file that will get scp to remote host
//	public boolean is_local = false;
	
	public ImageFile(String source_image, String guest_image, String work_image) {
		this.source_image = source_image;
		this.guest_image = guest_image;
		this.work_image = work_image;
	}
	
}
