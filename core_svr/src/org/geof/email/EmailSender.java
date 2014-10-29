package org.geof.email;

import java.util.HashMap;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.geof.prop.GlobalProp;

public class EmailSender {
	
	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String TO_ADDR = "to_addr";
	public static final String LOGIN = "login";
	public static final String PWD = "pwd";
	public static final String SUBJECT = "subject";
	public static final String MESSAGE = "msg";
	public static final String EMAIL = "email";


	public static void send(HashMap<String,String> data) {
 
		String username = data.get(LOGIN);
		String password = data.get(PWD);
		String to_addr = data.get(TO_ADDR);
		String host = data.get(HOST);
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.user", username);
	    props.put("mail.smtp.password", password);
 	    
		try {
			Session session = Session.getDefaultInstance(props, null);
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to_addr));
			message.setSubject(data.get(SUBJECT));
			message.setText(data.get(MESSAGE));
			Transport transport = session.getTransport("smtp");
			transport.connect(host, username, password);
		    transport.sendMessage(message, message.getAllRecipients());
		    transport.close();
  
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void sendEmail(String address, String subject, String message) throws Exception {
		
		String mail_login = GlobalProp.getProperty("mail_login");
		String mail_pwd = GlobalProp.getProperty("mail_pwd");
		String service_name = "smtp." + GlobalProp.getProperty("service_name");
		HashMap<String,String> mapEmail = new HashMap<String,String>();
		
		mapEmail.put(EmailSender.HOST,service_name);
//			mapEmail.put(EmailSender.PORT,email);
		mapEmail.put(EmailSender.TO_ADDR,address);
		mapEmail.put(EmailSender.LOGIN, mail_login);
		mapEmail.put(EmailSender.PWD,mail_pwd);
		mapEmail.put(EmailSender.SUBJECT,subject);
		mapEmail.put(EmailSender.MESSAGE,message);

		send(mapEmail);			
	}

}
