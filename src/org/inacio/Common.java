package org.inacio;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Common {
	final static Logger LOG = Logger.getLogger(Common.class.getName());
	
	public static double computeAmt(Member member,ArrayList<Individual> individuals) {
		double amt = 0f;
		double discAmt = 0f;
		if (member.getRate().equals("teen") && getAge(individuals.get(0).getDateOfBirth()) > 18) {
			member.setRate("single");
		}
		int adult = 0;
		int college = 0;
		int child = 0;
		int teen = 0;
		for(Individual i:individuals) {
			if(getAge(i.getDateOfBirth()) > 17) {adult++;}	
			else if(getAge(i.getDateOfBirth()) > 11) { teen++; }	
			else { child++; }			
			
			if(getAge(i.getDateOfBirth()) > 17 && getAge(i.getDateOfBirth()) < 23) college++;  // These are counted as adults and college
		}
		if (member.getRate().equals("family")) {
			amt = 295.00f;
			discAmt = -25.00f;
			if((adult-college) > 2) { amt += 195 * (adult - college - 2); }			// Reducing adult by value of college for the family calculation
			if((college+teen+child) > 4) { amt += 25 * (college + teen + child - 4); }  // Combining college teens and children for the family calculation
		} else if (member.getRate().equals("teen")) {
			amt = 75.00f;
			if(teen > 1) { amt += 75.0f * (teen - 1);}  //  Multiple teens on a single membership ??
			if(adult > 0) { amt += 195.0f * adult; } // Teen having others?
			if(child > 0) { amt += 25.0f * child; }  // Teen having others?
		} else if (member.getRate().equals("single")) {
			amt = 195.00f;
			discAmt = -20.00f;
			if(adult > 1) { amt += 195.0f * (adult-1); }  
			if(teen > 0) { amt += 75.0f * teen;}      // Single Parent plus teen $75 ??
			if(child > 0) { amt += 25.0f * child; }   //  Single Parent plus children  $25 ??
		} else if (member.getRate().equals("couple")) {
			amt = 250.00f;
			discAmt = -30.00f;
			if(adult > 2) { amt += 195.0f * (adult - 2);}
			if(teen > 0) { amt += 75.0f * teen; }
			if(child > 0) { amt += 25.0f * child; }  // Couple plus 1 child is $275 which is less than the family plan
		} else if (member.getRate().equals("senior")) {
			amt = 135.00f;
			discAmt = -15.00f;
			if(adult > 1) { amt += 195.0f * (adult - 1);}
			if(teen > 0) { amt += 75.0f * teen; }
			if(child > 0) { amt += 25.0f * child; }  // Senior plus 6 child is $285 which is less than the family plan			
		} else if (member.getRate().equals("seniorcouple")) {
			amt = 185.00f;
			discAmt = -35.00f;
			if(adult > 2) { amt += 195.0f * (adult - 2);}
			if(teen > 0) { amt += 75.0f * teen; }
			if(child > 0) { amt += 25.0f * child; }  // Senior Couple plus 4 child is $285 which is less than the family plan			
		} else if (member.getRate().equals("babysitter")) {
			amt = 60.00f;
			if(adult > 1) { amt += 195.0f * (adult - 1);}
			if(teen > 0) { amt += 75.0f * teen; }
			if(child > 0) { amt += 25.0f * child; }  // Senior Couple plus 4 child is $285 which is less than the family plan			
		}
		Calendar now = Calendar.getInstance();
		Calendar disc = Calendar.getInstance();
		disc.set(2017, 5, 23, 23, 59);
		if (disc.after(now)) {
			amt += discAmt;
		}
		return amt;
	}

	public static String getStyle() {
		StringBuilder sb = new StringBuilder();
		sb.append("html, body {height:100%;min-height:100vh;position:relative;background: linear-gradient(to bottom, #122b6b, #01b5ff 100%) no-repeat;margin: 0 auto;padding: 0;}");
		sb.append("header {min-height: 100px; min-width: 600px;max-width:800px;margin: 0px auto;background: #122b6b url('/images/logo.png') no-repeat scroll top center;}");
		sb.append("nav {min-height: 10px; min-width: 600px;max-width:800px;margin: 0px auto;background: blue;border: blue;border-radius: 18px 18px 18px 18px;}");
		sb.append("body {color:white;overflow hidden;}");
		sb.append(".registration {min-height: 100px; min-width: 600px;max-width:800px;margin: 0px auto;border: 1px solid yellow;border-radius: 18px 18px 0 0;}");
		sb.append(".registration table {width: 100%;}");
		sb.append(".registration tr:nth-child(odd){border: 2px solid yellow;}");
		sb.append(".receipt {min-height: 100px; min-width: 600px;max-width:800px;margin: 0px auto;border-radius: 18px 18px 0 0;border: 2px solid black;}");
		sb.append(".memberCard {height: 200px; width: 320px;margin: 0px auto;border: 2px solid red;background-color: yellow;color:black;}");
		sb.append(".memberCard td {color:black;}");
		sb.append(".memberCard img {height: 100px; width: 320px;}");
		sb.append(".label {text-align: right; vertical-align: bottom;}");
		sb.append(".input {text-align: left;vertical-align: bottom;}");
		sb.append(".family {}");
		sb.append(".single {}");
		sb.append(".couple {}");
		sb.append(".teen {}");
		sb.append(".babysitter {}");
		sb.append(".senior {}");
		sb.append(".seniorcouple {}");
		sb.append(".tblIndividuals th {vertical-align: bottom; text-align: left;}");
		sb.append(".minor {border: 4px solid blue;}");
		return sb.toString();
	}

	public static String getHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<head>");
		sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">")
		  .append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">")
		  .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"//fonts.googleapis.com/css?family=Fredericka+the+Great|Allura|Amatic+SC|Arizonia|Averia+Sans+Libre|Cabin+Sketch|Francois+One|Jacques+Francois+Shadow|Josefin+Slab|Kaushan+Script|Love+Ya+Like+A+Sister|Merriweather|Offside|Open+Sans|Open+Sans+Condensed|Oswald|Over+the+Rainbow|Pacifico|Romanesco|Sacramento|Seaweed+Script|Special+Elite\">")
		  .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"//fonts.googleapis.com/css?family=Open+Sans:300,400,600,700\">");
		sb.append("<style>");
		sb.append(getStyle());
		sb.append("</style>");
		sb.append("<script>");
		sb.append("function startup() {}");
		sb.append("</script>");
		sb.append("</head>");
		sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" \\>");
		sb.append("<title>").append(PoolMemberSignUpServlet.poolName).append("</title>");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js\"></script>");
		sb.append(
				"<link rel=\"stylesheet\" href=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/themes/smoothness/jquery-ui.css\">");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js\"></script>");
		sb.append(" <meta charset=\"utf-8\"/>");
		sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\" />");
		sb.append("</head>");
		return sb.toString();
	}
	
	public static int getAge(long l) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(l);
		return getAge(c);
	}

	public static int getAge(Calendar c) {
		Calendar now = Calendar.getInstance();
		int nowYr = now.get(Calendar.YEAR);
		int ageYr = c.get(Calendar.YEAR);
		int age = nowYr - ageYr;
		if (now.get(Calendar.MONTH) < c.get(Calendar.MONTH)) {
			age--;
		} else if (now.get(Calendar.MONTH) == c.get(Calendar.MONTH)
				&& now.get(Calendar.DATE) < c.getMaximum(Calendar.DATE)) {
			age--;
		}
		return age;
	}
	
	public static void sendEmail(String[] toEmail, String[] ccEmail, String[] bccEmail, String frmEmail, String subject, String content) {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		try {
		  Message msg = new MimeMessage(session);
		  if(frmEmail == null) { return; }
		  if(toEmail == null && ccEmail == null && bccEmail == null) {return;}
		  msg.setFrom(new InternetAddress(frmEmail));
		  for(String s:toEmail) {
			  msg.addRecipient(Message.RecipientType.TO, new InternetAddress(s, s));
		  }
		  if(ccEmail != null) {
			  for(String s:ccEmail) {
				  msg.addRecipient(Message.RecipientType.CC, new InternetAddress(s, s));
			  }
		  }
		  if(bccEmail != null) {
			  for(String s:bccEmail) {
				  msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(s, s));
			  }
		  }
		  msg.setSubject(subject);
		  Multipart mp = new MimeMultipart();
		  MimeBodyPart htmlPart = new MimeBodyPart();
		  htmlPart.setContent(content,"text/html");
		  mp.addBodyPart(htmlPart);
		  msg.setContent(mp);	 
		  Transport.send(msg);
		} catch (AddressException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		} catch (MessagingException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		} catch (UnsupportedEncodingException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}		
	}
}
