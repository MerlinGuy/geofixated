package org.geof.request;

import org.apache.commons.codec.binary.Base64;

public class TestRequest extends Request {

	@Override
	/*
	 * Processes Read and Delete calls from clients.
	 * 
	 * @return Returns true if the call was a Read or Delete and successfull otherwise false.
	 */
	public boolean process() {
		if (_actionKey.equalsIgnoreCase(READ)) {
			return read();
		} 
		return false;
	}

	/**
	 * Reads all active database connections and writes them to the response stream as a list.
	 * 
	 * @return Returns true if no errors occurr.
	 */
	private boolean read() {
		
		String rtn = "test";
		if (_fields.has("return_value")) {
			rtn = new String(Base64.decodeBase64( _fields.optString("return_value","")));
		}
		_jwriter.openArray(DATA);
		_jwriter.openObject();
		_jwriter.write( rtn );
		_jwriter.closeObject();
		_jwriter.closeArray();

		return true;
	}

}
