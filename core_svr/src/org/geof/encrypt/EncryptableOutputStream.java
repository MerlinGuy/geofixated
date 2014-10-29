package org.geof.encrypt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.geof.log.GLogger;

import org.apache.commons.codec.binary.Base64;

public class EncryptableOutputStream extends OutputStream {

	public final static int DEFAULT_BUFFER_SIZE = 64;
	private OutputStream _out;	
	private Cipher _cipher = null;
	private String _iv = null;
	private boolean _encrypt = false;
	private int _bufferSize = DEFAULT_BUFFER_SIZE;
	private byte[] _buffer = null;
	private int _bufferPos = 0;
    private final byte[] abyte = new byte[1];
	private FileOutputStream _debugStream = null;
	
	public static String LINEBREAK = ","; // "\n"
	private static String _padding = "===";
	
	public EncryptableOutputStream(OutputStream out) {
		this(out, DEFAULT_BUFFER_SIZE);
	}
	
	public EncryptableOutputStream(OutputStream out, int buffersize) {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		_bufferSize = buffersize;
		_buffer = new byte[_bufferSize];
		_out = out;
	}
	
	public void setDebugFile(String filepath) {
		try {
			_debugStream = new FileOutputStream(filepath);
		} catch (FileNotFoundException e) {
			GLogger.error(e);
		}
	}
	
	private void writeEncrypted( byte[] buffer, int offset, int len ) throws IOException {
		int remaining = len - offset;
		int copylen = 0;

		while ( remaining > 0 ) {			
			copylen = Math.min(remaining, _bufferSize - _bufferPos);
			System.arraycopy(buffer, offset, _buffer, _bufferPos, copylen);
			_bufferPos += copylen;
			if (_bufferPos == _bufferSize) {
				writeEncryptedBuffer();
			}
			offset += copylen;
			remaining = len - offset;
		}
	}
	
	private void writeEncryptedBuffer() throws IOException {
		try {
			if (_bufferPos > 0) {
				byte[] encrypted;
				try {
					encrypted = _cipher.doFinal(_buffer, 0, _bufferPos);
					_bufferPos = 0;
				} catch (Exception e) {
					throw new IOException(e.getMessage());
				}
				byte[] encoded = Base64.encodeBase64(encrypted, false, false);
				encoded = padEncoded(encoded);
				_out.write(encoded);
				_out.write(LINEBREAK.getBytes());
			}
			_out.flush();			
			
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}
	
	@Override
	public void flush() throws IOException {
		// This class only flushes at two point...
		// 1) The buffer is full
		// 2) the encyption is turned off.
	}
	
	private static byte[] padEncoded(byte[] encoded) {
		int padLen =  encoded.length % 4;
		if (padLen > 0) {
			byte[] pEncoded = new byte[encoded.length + padLen];
			System.arraycopy(encoded, 0, pEncoded, 0, encoded.length);
			System.arraycopy(_padding.getBytes(), 0, pEncoded, encoded.length, padLen);
			encoded = pEncoded;
		}
		return encoded;
	}
	
	public void write(String input) throws IOException {
		byte[] b = input.getBytes();
		this.write(b,0,b.length);
	}
	
	@Override
	public void write(int b) throws IOException {
		abyte[0] = (byte) b;
        write(abyte, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (_encrypt) {
			writeEncrypted(b,off,len);
		} else {
			_out.write(b,off,len);
		}
		if (_debugStream != null) {
			_debugStream.write(b,off,len);
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		this.write(b,0,b.length);
	}
	
	@Override
	public void close() throws IOException {
		if( _encrypt ) {
			writeEncryptedBuffer();
		}
		if (_debugStream != null) {
			_debugStream.write("\r\n\r\n".getBytes());
			_debugStream.close();
		}
		_out.flush();
		_out.close();
	}
	
	public String setupAesCipher( SecretKeySpec sks ) {
		try {
			byte [] iv = new byte[8];
			(new SecureRandom()).nextBytes(iv);
			_cipher = Cipher.getInstance(EncryptUtil.AES_CIPHER_TYPE, "BC"); 
			_cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(iv));
			_iv = Hex.encodeHexString(iv);
			return _iv;
		} catch (Exception e) {
			GLogger.error(e);
			return null;			
		}
	}
	
	public boolean encryptOutput( boolean encrypt) throws IOException {
		if (encrypt) {
			if (! _encrypt) {
				_encrypt = canEncrypt();
			}
		} else if ( _encrypt ) {
			writeEncryptedBuffer();
			_encrypt = false;
		}
		return _encrypt;
	}
	
	public boolean canEncrypt() {
		return _cipher != null;
	}
	
	public boolean isEncrypting() {
		return _encrypt;
	}

}
