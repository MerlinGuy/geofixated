package org.geof.tasker.im;

import java.io.IOException;
import java.io.OutputStream;

import org.geof.log.GLogger;
import org.geof.service.Transaction;
import org.geof.tasker.ARetrieveTask;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONException;
import org.json.JSONObject;

public class GIMTask extends ARetrieveTask {
	
	public final static String SERVICE_NAME = "service_name";
	private XMPPConnection _connection = null;
	
	public GIMTask() {}
	
	public void initialize(JSONObject joParams) {
		super.initialize(joParams);
		try {
			_data.put(SERVICE_NAME, joParams.getString(SERVICE_NAME));
		} catch (JSONException e) {
			GLogger.error(e.getMessage());
		}		
	}	
	
	public boolean start() {
		String host = (String)_data.get(HOST);
		Integer port = (Integer)_data.get(PORT);
		String login = (String)_data.get(LOGIN);
		String service_name = (String)_data.get(SERVICE_NAME);
		String pwd = (String)_data.get(PWD);
		_connection = null;

		GLogger.debug("GIMManager starting " + host);
		ConnectionConfiguration connConfig = new ConnectionConfiguration(host,port,service_name);
		_connection = new XMPPConnection(connConfig);
		
		try {
			_connection.connect();
			_connection.login(login, pwd);
		} catch (XMPPException ex) {
			GLogger.error(ex.getMessage());
			_error_msg = ex.getMessage();
			return false;
		}

		ChatManager chatmanager = _connection.getChatManager();
		chatmanager.addChatListener(new ChatManagerListenerImpl());
		GLogger.debug("GIMManager starting " + host + " is connected ");
		return true;
	}
	
	public void dispose() {
		this.disconnect();
	}

	public void disconnect() {
		if (_connection != null) {
			_connection.disconnect();
			GLogger.debug("GIMManager " + _data.get(HOST).toString() + " now disconnected ");
		}
	}
	
	public String serviceName() {
		return _data.get(SERVICE_NAME).toString();
	}
	
	private class ChatManagerListenerImpl implements ChatManagerListener {

		@Override
		public void chatCreated(final Chat chat, final boolean createdLocally) {
			MessageListenerImpl mli = new MessageListenerImpl();
			chat.addMessageListener(mli);
		}
	}
	
	private class MessageListenerImpl implements MessageListener {

		@Override
		public void processMessage(Chat chat, Message message) {
			try {
				if ((message != null) && (message.getBody() != null)) {
					String jsonString =  message.getBody();	
					Transaction transaction = new Transaction();
//					EncryptableOutputStream outStream = new EncryptableOutputStream(new DevNull());
//					boolean rtn = transaction.process(new JSONObject(jsonString), outStream);
					boolean rtn = transaction.process(jsonString, new DevNull());
					if (rtn) {
						chat.sendMessage("Okay");
					} else {
						chat.sendMessage(transaction.getError());
					}
				}
			} catch (Exception e) {
				GLogger.error("GIMManager.processMessage: " + e.getMessage());
			}
		}
	}

	private class DevNull extends OutputStream {
		@Override
		public void write(int arg0) throws IOException {
		}		
	}

}
