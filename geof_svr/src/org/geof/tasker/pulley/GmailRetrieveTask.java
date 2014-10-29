package org.geof.tasker.pulley;

import org.geof.log.GLogger;
import org.geof.tasker.PulleyTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

public class GmailRetrieveTask extends PulleyTask {

	public GmailRetrieveTask() {
		super();
	}

	@Override
	public void run() {
		Store store = null;
		Folder inbox = null;

		_downloads = new JSONArray();
		try {

			String host = (String)_data.get(HOST);
			Integer port = (Integer)_data.get(PORT);
			String login = (String)_data.get(LOGIN);
			String pwd = (String)_data.get(PWD);
			String downloaddir = (String)_data.get(DOWNLOAD_DIR);
			String name = (String)_data.get(NAME);
			boolean delete_after_download = (Boolean)_data.get(DELETE_AFTER_DOWNLOAD);

			// Create empty properties
			Properties props = new Properties();

			props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.setProperty("mail.imap.socketFactory.fallback", "false");

			props.setProperty("mail.imaps.partialfetch", "false");

			props.setProperty("mail.imap.host", host); // "imap.gmail.com"
			props.setProperty("mail.imap.port", Integer.toString(port));
			props.setProperty("mail.imap.connectiontimeout", "5000");
			props.setProperty("mail.imap.timeout", "5000");

			Session session = Session.getInstance(props, null);
			store = session.getStore("imaps");
			store.connect(host, login, pwd);
			inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);
			
			Folder trash = store.getFolder("[Gmail]/Trash");

			// Get directory listing
			Message msgs[] = inbox.getMessages();
			for (Message msg : msgs) {
				boolean move_to_trash = false;
				String bodyText = "";
				// String fromId = msg.getFrom()[0].toString();
				Date receivedDate = msg.getReceivedDate();

				if (receivedDate == null) {
					receivedDate = new Date();
				}
				String subject = msg.getSubject();

				boolean hasKeyInSubject = false;
				String[] subjects = _data.get(SUBJECTS).toString().split(",");
				for (String key : subjects) {
					if (subject.indexOf(key) > -1) {
						// GLogger.debug("Subject: " + subject);
						hasKeyInSubject = true;
						break;
					}
				}
				if (!hasKeyInSubject) {
					continue;
				}

				Object content = msg.getContent();
				if (content instanceof Multipart) {

					Multipart mp = (Multipart) content;
					int count = mp.getCount();
					for (int j = 0; j < count && (! move_to_trash); j++) {

						Part part = mp.getBodyPart(j);
						String dispo = part.getDisposition();

						if (dispo == null) {

							MimeBodyPart mbp = (MimeBodyPart) part;
							if (mbp.isMimeType("text/plain")) {
								// Plain
								bodyText += (String) mbp.getContent();
							} else if (mbp.isMimeType("multipart/alternative")) {
								// prefer html text over plain text
								String text = null;
								if (part.isMimeType("text/plain")) {
									if (text == null)
										bodyText += (String) part.getContent();
									continue;
								} else if (part.isMimeType("text/html")) {
									bodyText += (String) part.getContent();
								} else {
									bodyText = readBodyText(part);
									// GLogger.debug("bodyText:" + bodyText);
								}
							}

						} else if (dispo.equalsIgnoreCase(Part.ATTACHMENT)
								|| dispo.equals(Part.INLINE)) {

							// Check if plain
							MimeBodyPart mbp = (MimeBodyPart) part;
							if (mbp.isMimeType("text/plain")) {
								bodyText += (String) mbp.getContent();

							} else {
								String ename = decodeName(part.getFileName());

								String guid = java.util.UUID.randomUUID().toString();
								JSONObject joPFI = null;
								if (bodyText.length() > 0) {
									joPFI = new JSONObject(bodyText);
								} else {
									joPFI = new JSONObject();
								}
								joPFI.put("task", this.getClass().getName());
								joPFI.put("host", host);
								joPFI.put("login", login);
								joPFI.put("originalname", ename);
								joPFI.put("receiveddate",
										DateFormat.getInstance().format(receivedDate));
								joPFI.put("json", new JSONObject(bodyText));
								joPFI.put("savedirectory", downloaddir);
								joPFI.put("filename", guid);

								File afile = new File(downloaddir, guid);

								// GLogger.debug("Attachment: " + ename);
								JSONObject fileInfo = saveFile(afile, part);
								int filesize = fileInfo.getInt("filesize");
								if (filesize == -1) {
									String emsg = "%s - Error saving file: %s to $s";
									GLogger.error(emsg, name, ename, afile.getAbsolutePath());
								}
								joPFI.put("filesize", filesize);
								joPFI.put("checksumval", fileInfo.getString("checksumval"));
								if ((_taskMgr != null) && _taskMgr.hasFile(joPFI)) {
									GLogger.verbose("GRT: has file - " + ename);
									if (delete_after_download) {
										move_to_trash = true;
									}
								} else {
									_downloads.put(joPFI);
								}
							}
						}
					}
				} // end messages for loop
				if (move_to_trash) {
				    inbox.copyMessages(new Message[] { msg }, trash);
//					msg.setFlag(Flags.Flag.DELETED, true);
				}
			}
			
			processDownloads();

		} catch (Exception e) {
			// TODO: convert this to simple error message after development
			// complete
			GLogger.error(e);
		} finally {
			if (inbox != null) {
				try {
					inbox.close(true);
				} catch (Exception e) {
				}
			}
			if (store != null) {
				try {
					store.close();
				} catch (Exception e) {
				}
			}
		} // end Try/Catch/Finally
	} // end retrieve()

	@Override
	public void dispose() {
	}
}