package org.inacio;

import java.util.ArrayList;
import java.util.Calendar;

public class Common {
	
	public static double computeAmt(Member member,ArrayList<Individual> individuals) {
		double amt = 0f;
		double discAmt = 0f;
		if (member.getRate().equals("teen") && getAge(individuals.get(0).getDateOfBirth()) > 18) {
			member.setRate("single");
		}
		if (member.getRate().equals("family")) {
			amt = 295.00f;
			discAmt = -25.00f;
		} else if (member.getRate().equals("teen")) {
			amt = 75.00f;
			if (individuals.size() > 1) {
				amt = 75.00f * individuals.size();
			}
		} else if (member.getRate().equals("single")) {
			amt = 195.00f;
			if (individuals.size() > 1) {
				amt = 195.00f * individuals.size();
			}
			discAmt = -20.00f;
		} else if (member.getRate().equals("couple")) {
			amt = 250.00f;
			if (individuals.size() > 2) {
				amt = 250.00f + (195.00f * (individuals.size() - 2));
			}
			discAmt = -30.00f;
		} else if (member.getRate().equals("senior")) {
			amt = 135.00f;
			if (individuals.size() > 1) {
				amt = 135.00f * individuals.size();
			}
			discAmt = -15.00f;
		} else if (member.getRate().equals("seniorcouple")) {
			amt = 185.00f;
			if (individuals.size() > 2) {
				amt = 185.00f + (135.00f * (individuals.size() - 2));
			}
			discAmt = -35.00f;
		} else if (member.getRate().equals("babysitter")) {
			amt = 60.00f;
			if (individuals.size() > 1) {
				amt = 195.00f * individuals.size();
			}
		}
		Calendar now = Calendar.getInstance();
		Calendar disc = Calendar.getInstance();
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
}
