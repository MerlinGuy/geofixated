package org.geof.service;

import java.io.OutputStream;
import java.security.Security;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.geof.service.GSession;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.util.encoders.Hex;
import org.geof.db.DBConnMgr;
import org.geof.db.DBConnection;
import org.geof.db.DBInteract;
import org.geof.encrypt.CipherMgr;
import org.geof.encrypt.EncryptUtil;
import org.geof.encrypt.EncryptableOutputStream;
import org.geof.log.GLogger;
import org.geof.prop.GlobalProp;
import org.geof.request.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.commons.codec.binary.Base64;

/**
 * Transaction Class is the management object for each client communication with
 * the server.
 * 
 * @author Jeff Boehmer
 * @comanpay Ft Collins Research, LLC.
 * @url www.ftcollinsresearch.org
 * 
 */
public class Transaction {
	// Static values
	public static final String TID = "tid";
	public static final String DEBUG = "debug";
	public static final String ENTITYNAME = "entity";
	public static final String TRANSACTION = "tid";
	public static final String TCALLBACK = "tcallback";
	public static final String SESSIONID = "sid";
	public static final String TOKEN = "token";
	public static final String REQUESTS = "requests";
	public static final String CIPHER_TYPE = "cipher_type";
	public static final String ENCRYPTID = "encryptid";
	public static final String NO_REQUESTS_ERROR = "No Requests sent";
	public static final String PAYLOADS = "payloads";
	public static final String PAYLOADS_ENCRYPTED = "payload_encrypted";
	public static final String ORDER = "order";
	
	public static final String RSA = "rsa";
	public static final String AES = "aes";
	public static final String IV = "iv";

	protected static String _sqlAuditRequest = "INSERT INTO requestaudit (requestname,actionname,actionas,usrid,sessionid,result,rundate,completedate) VALUES (?,?,?,?,?,?,?,?)";

	protected static int _count = 0;

	// life cycling objects
	protected GSession _session = null;
	protected DBConnection _conn = null;
	protected DBInteract _dbi = null;
	protected JsonWriter _jwriter = null;
	protected EncryptableOutputStream _outStream = null;
	protected ArrayList<JSONObject> _requests = null;
	protected String _ivEncrypt = null;
	protected String _ivDecrypt = null;

	protected JSONObject _transaction = null;
	protected HashMap<String, JSONObject> _returning = new HashMap<String, JSONObject>();;
	protected HashMap<String, Object> _reference = null;
	protected JSONObject _sendback_rko = null;
	protected JSONObject _payloads = null;
	private AsymmetricBlockCipher _cipherRSA = null;
	private Cipher _cipherAES = null;
	
	// header parameters
	protected long _usr_id = -1;
	private int _id = 0;
	protected String _errMsg = null;
	private boolean _hasTerminalRequestError = false; 

	protected JSONObject _curRequest = null;
	private static List<String> auditIgnores = Arrays.asList(GlobalProp.getProperty("do_not_audit").split(","));

	private final String _debugPath = "/var/log/geof/eoutput.log";
	/**
	 * Method processes all of the Request objects sent in the current
	 * transaction.
	 * 
	 * @return True if method succeeds otherwise false if an error occurs
	 */
	public boolean process(String transaction, OutputStream outStream) {

		long curTime = (new Date()).getTime();
		try {
			_transaction = new JSONObject(transaction);
			_id = _transaction.optInt(TID, -1);
		} catch (Exception e) {
			_errMsg = "malformed Transaction JSON";
		}
		
		try {
			_outStream = new EncryptableOutputStream(outStream);
			
			if ( _transaction.optBoolean(DEBUG, false)) {				
				_outStream.setDebugFile(_debugPath);
			}
			
			_jwriter = new JsonWriter(_outStream);
			_jwriter.startWrite(curTime, _transaction.optInt(TRANSACTION, _count++));
			
			if (_transaction.has(TOKEN)) {
				String token = _transaction.getString(TOKEN);
				_session = SessionMgr.getSessionByToken(token);
				if (_session == null) {
					return setError("Invalid session token");
				}
				
			} else if (_transaction.has(SESSIONID)) {
				String sessionkey = _transaction.getString(SESSIONID);
				_session = SessionMgr.getSession(sessionkey);
				if (_session == null) {
					return setError("Invalid session id");
				}
			}
			

			_conn = DBConnMgr.getConnection(_session);
			_dbi = new DBInteract(this);
			
			if (_session != null) {
				_session.setActive(true);
			}
			
			_payloads = _transaction.optJSONObject(PAYLOADS);

			JSONArray requests = null;

			String cipher_type = _transaction.optString(CIPHER_TYPE,"");
			if (cipher_type.length() > 0) {
				setupEncryption( cipher_type );
				
				String encReqs = _transaction.optString(REQUESTS,"");
				if (encReqs.length() > 0) {
					try {
						requests = new JSONArray(decrypt(encReqs, cipher_type));
						decryptPayloads(cipher_type);
					} catch (Exception ed){
						return setError("Uncaught decryption error: " + ed.getMessage());
					}
					if (_sendback_rko != null) {
						_jwriter.writePair("rsa", _sendback_rko);
					}
					_jwriter.writePair(CIPHER_TYPE, cipher_type);
					if (_ivEncrypt != null) {
						_jwriter.writePair(IV, _ivEncrypt);
					}
				}
			} else {
				requests = _transaction.optJSONArray(REQUESTS);
			}
			
			boolean rtn = false;
			if (requests == null || requests.length() == 0) {
				_errMsg = NO_REQUESTS_ERROR;
			} else {
				_jwriter.openRequests();
				sortRequests(requests);
				int reqCount = _requests.size();
				int iRequest = 0;
				while ((iRequest < reqCount) && canProcess()) {
					this.processRequest( _requests.get( iRequest++ ) );
				}				
				rtn = canProcess();
			}
			return rtn;
		} catch (Exception e) {
			_errMsg = e.getMessage();
			GLogger.error(e);
			return false;

		} finally {
			_jwriter.completeWrite(curTime, _errMsg);
			DBConnMgr.release(_conn);
			if (_session != null) {
				_session.setActive(false);
			}
		}
	}
	
	private boolean processRequest(JSONObject joRequest) throws Exception {
		_curRequest = joRequest;
		Request objRequest = Request.getInstance(_curRequest, this);
		if (objRequest == null) {
			return false;
		}
		boolean rtn = objRequest.executeRequest();
		auditRequest(objRequest, rtn, new Timestamp((new Date()).getTime()));
		return rtn;
	}

	private void setupEncryption(String cipher_type ) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		if (cipher_type.equalsIgnoreCase(RSA)) {
			int encryptID = _transaction.optInt(ENCRYPTID, -1);
			_cipherRSA = CipherMgr.getRsa(encryptID, _dbi);
			
		} else if (cipher_type.equalsIgnoreCase(AES)) {
			if (_session == null) {
				return;
//				throw new Exception("setupEncryption AES: " + Request.NO_SESSION_ERROR);
			}
			SecretKeySpec secretKeySpec = _session.getAesKeySpec();
			if (secretKeySpec == null) {
				throw new Exception("setupEncryption AES: Session is missing AES Key Spec");
			}
			String str_iv = _transaction.optString(IV, null);
			byte[] iv = Hex.decode(str_iv);
			if (str_iv == null) {
				throw new Exception("setupEncryption AES: Session is missing AES iv");
			}
			_cipherAES = Cipher.getInstance(EncryptUtil.AES_CIPHER_TYPE, "BC");
			_cipherAES.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));

			_ivDecrypt = _transaction.getString(IV);
			_ivEncrypt = _outStream.setupAesCipher(secretKeySpec);
		}
	}
	
	private String decrypt(String encryptedText, String cipher_type) throws Exception {
		if (cipher_type.equalsIgnoreCase(RSA)) {
			return decryptRSA(encryptedText);
			
		} else if (cipher_type.equalsIgnoreCase(AES)) {
			return decryptAES(encryptedText);
		} else {
			throw new Exception("Transaction.decrypt: Unknown cipher type: " + cipher_type);
		}
	}

	private void decryptPayloads(String cipher_type)throws Exception {
		if (_payloads == null || _transaction.optBoolean(PAYLOADS_ENCRYPTED) == false) {
			return;
		}
		String[] pRIds = JSONObject.getNames(_payloads);
		if (pRIds == null) {
			return;
		}
		for (String pRid : pRIds) {
			String payload = decrypt(_payloads.getString(pRid), cipher_type);
			_payloads.put(pRid, payload);
		}
	}

	private String decryptRSA( String encryptedText ) throws Exception {
		String[] eBlocks = encryptedText.split(",");
		String str = "";
		byte[] hReqs;
		for (String block : eBlocks) {
			hReqs = Hex.decode(block);
			str += new String(_cipherRSA.processBlock(hReqs, 0, hReqs.length));
		}
		return str;
	}
	
	private String decryptAES( String encryptedText )  throws Exception {
		byte[] encbytes = (new Base64()).decode(encryptedText);
		byte[] byteRequests = _cipherAES.doFinal(encbytes);
		return new String(byteRequests);
	}
	
	public JSONObject getReturning(String key) {
		return _returning.get(key);
	}

	public Object getReturning(String key, String element, Object dftValue) {
		if (_returning.containsKey(key)) {
			JSONObject returning = _returning.get(key);
			if (returning.has(element)) {
				try {
					return returning.get(element);
				} catch (JSONException e) {
					return dftValue;
				}
			} else {
				return dftValue;
			}
		} else {
			return dftValue;
		}
	}

	public Object getReturning(String code, Object dftValue) {
		String[] keys = code.split(",");
		if (_returning.containsKey(keys[0])) {
			JSONObject returning = _returning.get(keys[0]);
			if (returning.has(keys[1])) {
				try {
					return returning.get(keys[1]);
				} catch (JSONException e) {
					return dftValue;
				}
			} else {
				return dftValue;
			}
		} else {
			return dftValue;
		}
	}

	public void setReturning(String key, JSONObject returning) {
		_returning.put(key, returning);
	}

	/**
	 * Writes the current request information into the database audit table
	 */
	private void auditRequest(Request req, boolean result, Timestamp rundate) {
		try {
			String entity = req.getEntity();
			String action = req.getAction();
			String actionAs = req.getActionAs();
			if (auditIgnores.contains(entity + "." + action)) {
				return;
			}

			PreparedStatement ps = _conn.getPreparedStatement(_sqlAuditRequest);
			String sessionid = "unknown";
			long usrid = req.getUsrId();
			if (_session != null) {
				usrid = _session.getUsrID();
				sessionid = _session.getSessionID();
			}
			ps.setString(1, entity);
			ps.setString(2, action);
			ps.setString(3, actionAs);
			ps.setLong(4, usrid);
			ps.setString(5, sessionid);
			ps.setInt(6, result ? 1 : 0);
			ps.setTimestamp(7, rundate);
			ps.setTimestamp(8, new Timestamp((new Date()).getTime()));
			ps.execute();
			ps.close();
		} catch (SQLException e) {
			GLogger.error(e);
		}
	}

	/**
	 * 
	 * @return Returns the current session variable
	 */
	public GSession getSession() {
		return _session;
	}

	public void setSession(GSession session) {
		_session = session;
		_usr_id = _session.getUsrID();
	}

	/**
	 * 
	 * @return Return the current session id if session variable is set
	 *         otherwise returns null
	 */
	public String getSessionID() {
		return _session != null ? _session.getKey() : "";
	}

	public boolean canProcess() {
		return (!_hasTerminalRequestError) && (_errMsg == null);
	}
	
	public void setTerminalRequestError() {
		_hasTerminalRequestError = true;
	}
	/**
	 * 
	 * @return Returns the RequestObject currently being processed.
	 */
	public JSONObject getRequest() {
		return _curRequest;
	}

	public String getPayload(String key) throws Exception {
		if (_payloads != null && _payloads.has(key)) {
			return _payloads.getString(key);
		}
		throw new Exception("Transaction payloads doesn't contain key: [" + key + "]");
	}
	/**
	 * 
	 * @return Returns the DBConnection object
	 */
	public DBConnection getConnection() {
		return _conn;
	}

	/**
	 * 
	 * @return Returns DBInteract object used for accessing the database
	 */
	public DBInteract getDBInteract() {
		return _dbi;
	}

	/**
	 * 
	 * @return Returns the JsonWriter for writing data back to the response
	 *         stream
	 */
	public JsonWriter getStream() {
		return _jwriter;
	}

	/**
	 * 
	 * @return Returns the current Transaction id.
	 */
	public int getID() {
		return _id;
	}

	/**
	 * 
	 * @return Returns the usr id associated with the current session
	 */
	public long getUsrID() {
		return _usr_id;
	}

	/**
	 * Sets the current usr_id
	 * 
	 * @param usr_id
	 *            New long value to use for the usr_id
	 */
	public void setUsrID(long usr_id) {
		_usr_id = usr_id;
	}

	/**
	 * 
	 * @return Returns true if transaction has an error otherwise false
	 */
	public boolean hasError() {
		return _errMsg != null;
	}

	/**
	 * 
	 * @return Returns the current error message
	 */
	public String getError() {
		return _errMsg;
	}

	/**
	 * Sets the current error message
	 * 
	 * @param error
	 *            New error message to use.
	 */
	public boolean setError(String error) {
		_errMsg = error;
		return false;
	}

	/**
	 * Builds a sorted list of JSONObject requests in sorted order of execution
	 * according to the 'order' field of the request object. If no order field
	 * is supplied the request is assigned the order zero.
	 * 
	 * @param requests
	 *            JSONArray of requests to sort
	 */
	private void sortRequests(JSONArray requests) {
		_requests = new ArrayList<JSONObject>();
		try {
			JSONObject request;
			int order;
			for (int indx = 0; indx < requests.length(); indx++) {
				request = requests.getJSONObject(indx);
				order = request.optInt( ORDER, 0 );
				Integer iLoc = 0;
				for (iLoc = 0; iLoc < _requests.size(); iLoc++) {
					if (order < _requests.get(iLoc).optInt(ORDER, 0)) {
						break;
					}
				}
				_requests.add(iLoc, request);
			}
		} catch (JSONException e) {
			GLogger.error(e);
		}
	}

}
