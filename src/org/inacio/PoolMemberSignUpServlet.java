package org.inacio;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;

import com.google.appengine.api.datastore.KeyFactory;

public class PoolMemberSignUpServlet extends HttpServlet {
	private final SimpleDateFormat dteFmt = new SimpleDateFormat("MM/dd/yyyy");
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String poolName = "Wedgewood Hills Swim Club";
	private final String poolAddress = "20 Diamond St.";
	private final String poolCSZ = "Harrisburg, PA 17111";

	final static Logger LOG = Logger.getLogger(PoolMemberSignUpServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		process(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		process(req, resp);
	}

	public void process(HttpServletRequest req, HttpServletResponse resp) {
		String action = req.getParameter("action");
		if (action == null) {
			index(req, resp);
		} else if (action.equals("register")) {
			regInfo(req, resp); // Add new records to Member and Individuals and calls subTotal
		} else if (action.equals("process")) {
			processPayment(req, resp); // Redirects to Paypal for Payment
		} else if (action.equals("payment")) {
			receivePayment(req, resp); // Records Payment
		} else {
			subTotal(req, resp); // Handles cancellations
		}
	}

	public void regInfo(HttpServletRequest req, HttpServletResponse resp) {
		Map map = req.getParameterMap();
		Member member;
		ArrayList<Individual> individuals;
		member = new Member();
		member.setId(null);
		member.setAddress(((String[]) map.get("address"))[0].trim());
		member.setCsz(((String[]) map.get("csz"))[0].trim());
		member.setEmail(((String[]) map.get("email"))[0].trim());
		member.setName(((String[]) map.get("name"))[0].trim());
		member.setPhone(((String[]) map.get("phone"))[0].trim());
		member.setRate(((String[]) map.get("rate"))[0].trim());
		com.google.appengine.api.datastore.Key k = member.add();
		//String keyStr = KeyFactory.keyToString(k);
		member.setId(k);
		individuals = new ArrayList<Individual>();
		for (int i = 0; i < 6; i++) {
			Individual ind = new Individual();
			String inddobmm = ((String[]) map.get("inddobmm" + i))[0].trim();
			String inddobdd = ((String[]) map.get("inddobdd" + i))[0].trim();
			String inddobyyyy = ((String[]) map.get("inddobyyyy" + i))[0].trim();
			Calendar c = Calendar.getInstance();
			try {
				int mm, dd, yyyy;
				mm = Integer.parseInt(inddobmm);
				dd = Integer.parseInt(inddobdd);
				yyyy = Integer.parseInt(inddobyyyy);
				c.set(yyyy, mm, dd);
			} catch (NumberFormatException e) {
				c = null;
			}
			ind.setDateOfBirth(c);
			ind.setGender(((String[]) map.get("indgender" + i))[0]);
			ind.setName(((String[]) map.get("indname" + i))[0].trim());
			ind.setMemberId(member.getId());
			ind.setId(IndividualIO.add(ind));
			if (!ind.getName().trim().isEmpty()) {
				individuals.add(ind);
			}
		}
		member.setTotalAmt(computeAmt(member,individuals));
		member.add();
		String htmlContent = subTotal(member,individuals);
		ServletOutputStream out;
		try {
			out = resp.getOutputStream();
			out.print(htmlContent);
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public void subTotal(HttpServletRequest req, HttpServletResponse resp) {
		String id = req.getParameter("id");
		Member member = MemberIO.get(id);
		//String keyStr = KeyFactory.keyToString(member.getId());
		ArrayList<Individual> individuals = new ArrayList<Individual>();		
		individuals = IndividualIO.getIndividualsOfMember(member.getId());
		String htmlContent = subTotal(member,individuals);
		ServletOutputStream out;
		try {
			out = resp.getOutputStream();
			out.print(htmlContent);
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}		
	}
	
	public String subTotal(Member member,ArrayList<Individual> individuals) {
		StringBuilder sb = new StringBuilder();
		sb.append(getHeader());
		sb.append("<body><a href=\"http://www.wedgewoodhillsswimclub.com\"><header></header></a><nav></nav>")
		  .append("<div class=registration>");
		sb.append("<table>");
		sb.append("<tr><td>Name:</td><td>").append(member.getName()).append("</td></tr>");
		sb.append("<tr><td>Address:</td><td>").append(member.getAddress()).append("</td></tr>");
		sb.append("<tr><td>City State Zip</td><td>").append(member.getCsz()).append("</td></tr>");
		Calendar cal = Calendar.getInstance();
		sb.append("<tr><td>Rate as of ").append(dteFmt.format(cal.getTime())).append(":</td><td>")
				.append(DecimalFormat.getCurrencyInstance().format(member.getTotalAmt())).append("</td></tr>");
		sb.append("<tr><td colspan=2>Individuals</td></tr>");
		for (int i = 0; i < individuals.size(); i++) {
			if (individuals.get(i).getDob() == -1l) {
				sb.append("<tr><td colspan=2>").append(individuals.get(i).getName()).append(" [")
						.append(individuals.get(i).getGender()).append("]</td></tr>");
			} else {
				sb.append("<tr><td colspan=2>").append(individuals.get(i).getName()).append(" [")
						.append(individuals.get(i).getGender()).append(" ")
						.append(getAge(individuals.get(i).getDateOfBirth())).append("]</td></tr>");
			}
		}
		sb.append("</table>");
		sb.append("<form method=POST>");
		sb.append("<input type=submit value=\"Pay with Credit Card\">");
		sb.append("<input type=hidden name=id value=").append(KeyFactory.keyToString(member.getId())).append(" />");
		sb.append("<input type=hidden name=action value=process />");
		sb.append("</form>");
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}

	public void processPayment(HttpServletRequest req, HttpServletResponse resp) {
		HttpSession sess = req.getSession();
		APICredential credentialObj = new APICredential();
		Member member = MemberIO.get(req.getParameter("id"));
		String keyStr = KeyFactory.keyToString(member.getId());
		if (sess.getAttribute("credentialObj") == null) {
			credentialObj = initializePayPal();
		} else {
			credentialObj = (APICredential) sess.getAttribute("credentialObj");
		}
		String PPEnvironment = getServletConfig().getInitParameter("PPEnvironment");
		ServiceEnvironment PPEnv = ServiceEnvironment.PRODUCTION;
		if (PPEnvironment != null) {
			if (PPEnvironment.equals("BETA_SANDBOX")) {
				PPEnv = ServiceEnvironment.BETA_SANDBOX;
			} else if (PPEnvironment.equals("PRODUCTION")) {
				PPEnv = ServiceEnvironment.PRODUCTION;
			} else if (PPEnvironment.equals("SANDBOX")) {
				PPEnv = ServiceEnvironment.SANDBOX;
			} else {
				PPEnv = ServiceEnvironment.SANDBOX;
			}
		}
		LOG.log(Level.INFO, "Servlet initialized successfully");

		SimplePay simplePayment = new SimplePay();
		StringBuilder url = new StringBuilder();
		url.append(req.getRequestURL());
		String returnURL = url.toString() + "?id=" + keyStr + "&payId=" + keyStr + "&action=payment";
		String cancelURL = url.toString() + "?id=" + keyStr + "&payId=" + keyStr + "&action=cancel";
		simplePayment.setCancelUrl(cancelURL);
		simplePayment.setReturnUrl(returnURL);
		simplePayment.setCredentialObj(credentialObj);
		simplePayment.setUserIp(req.getRemoteAddr());
		simplePayment.setApplicationName("Wedgewood Hills Registration");
		simplePayment.setCurrencyCode(CurrencyCodes.USD);
		simplePayment.setEnv(PPEnv);
		simplePayment.setLanguage("en_US");
		simplePayment.setMemo("Wedgewood Hills Registration");

		// set the receiver
		Receiver primaryReceiver = new Receiver();
		primaryReceiver.setAmount(member.getTotalAmt());
		primaryReceiver.setEmail(credentialObj.getAccountEmail());
		primaryReceiver.setPaymentType(PaymentType.SERVICE);
		simplePayment.setReceiver(primaryReceiver);

		PayResponse payResponse = null;
		try {
			payResponse = simplePayment.makeRequest();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Payment Failure: " + e.getMessage());
		}
		if (payResponse != null && payResponse.getPaymentExecStatus() == PaymentExecStatus.CREATED) {
			member.setTransactionId(payResponse.getPayKey());
			member.setDatePaid(Calendar.getInstance().getTimeInMillis());
			member.add();
			String authSite;
			if (PPEnv.equals(ServiceEnvironment.SANDBOX)) {
				authSite = "https://www.sandbox.paypal.com/cgi-bin/webscr";
			} else {
				authSite = "https://www.paypal.com/cgi-bin/webscr";
			}
			LOG.info("Redirecting for Authorization: " + authSite + "?cmd=_ap-payment&paykey="
					+ payResponse.getPayKey());
			try {
				resp.sendRedirect(
						resp.encodeRedirectURL(authSite + "?cmd=_ap-payment&paykey=" + payResponse.getPayKey()));
			} catch (IOException e) {
				LOG.log(Level.SEVERE, e.getMessage());
			}
		}
		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append(getHeader());
		if (payResponse != null && payResponse.getPayKey() != null) {
			LOG.log(Level.INFO, "Payment success - payKey:" + payResponse.getPayKey());
			htmlContent.append("<h2>Payment Processed:" + payResponse.getPaymentExecStatus().toString() + "</h2>");
		} else {
			StringBuilder errMsg = new StringBuilder();
			if (payResponse != null) {
				for (int i = 0; i < payResponse.payErrorList.size(); i++) {
					errMsg.append(payResponse.payErrorList.get(i));
				}
			} else {
				errMsg.append("PayResponse is null");
			}
			LOG.log(Level.INFO, "Payment failure: " + errMsg.toString());
			htmlContent.append("<h2>Payment Failure: " + errMsg.toString() + "</h2>");
		}
		ServletOutputStream out;
		try {
			out = resp.getOutputStream();
			out.print(htmlContent.toString());
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}

	public void receivePayment(HttpServletRequest req, HttpServletResponse resp) {
		// HttpSession sess = req.getSession();
		StringBuilder sb = new StringBuilder();
		Member member = new Member();
		String id = req.getParameter("id");
		member = MemberIO.get(id);
		ArrayList<Individual> individuals = IndividualIO.getIndividualsOfMember(member.getId());
		String paypal = req.getParameter("payId");
		member.setTransactionId(paypal);
		member.setDatePaid(Calendar.getInstance().getTimeInMillis());
		member.add();
		sb.append(getHeader());
		sb.append("<body>");
		sb.append("<div class=receipt><table>");
		sb.append("<tr><td>").append(poolName).append("</td><td>&nbsp;</td></tr>");
		sb.append("<tr><td>").append(poolAddress).append("</td><td>&nbsp;</td></tr>");
		sb.append("<tr><td>").append(poolCSZ).append("</td><td>&nbsp;</td></tr>");
		sb.append("<tr><td colspan=2>&nbsp;</td></tr>");
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dteFmt = new SimpleDateFormat("MM/dd/yyyy");
		sb.append("<tr><td>").append(member.getName().trim()).append("</td><td>")
				.append(formatter.format(member.getTotalAmt())).append("</td></tr>");
		sb.append("<tr><td>&nbsp;</td><td>").append(dteFmt.format(cal.getTime())).append("</td></tr>");
		String transId = member.getTransactionId();		
		sb.append("<tr><td>&nbsp;</td><td>").append((transId)).append("</td></tr>");
		sb.append("<tr><td>&nbsp;</td><td>Member Id: E-").append(id.substring(id.length()-5)).append("</td></tr>");
		sb.append("</table></div>");
		Individual ind;
		for (int i = 0; i < individuals.size(); i++) {
			ind = individuals.get(i);
			sb.append("<div class=\"memberCard ").append(member.getRate().toUpperCase());
			if (getAge(ind.getDateOfBirth()) < 18) {
				sb.append(" minor");
			}
			sb.append("\">");
			sb.append("<table>");
			sb.append("<tr><td>").append(ind.getName()).append("</td></tr>");
			sb.append("</table>");
			sb.append("</div>");
		}
		sb.append("</body></html>");
		ServletOutputStream out;
		try {
			out = resp.getOutputStream();
			out.print(sb.toString());
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}

	public APICredential initializePayPal() {
		// Obtain the credentials from your configs
		APICredential credentialObj = new APICredential();
		credentialObj.setAPIUsername(getServletConfig().getInitParameter("PPAPIUsername"));
		credentialObj.setAPIPassword(getServletConfig().getInitParameter("PPAPIPassword"));
		credentialObj.setSignature(getServletConfig().getInitParameter("PPAPISignature"));

		// setup your AppID from X.com
		credentialObj.setAppId(getServletConfig().getInitParameter("PPAppID"));

		// setup your Test Business account email
		// in most cases this would be associated with API Credentials
		credentialObj.setAccountEmail(getServletConfig().getInitParameter("PPAccountEmail"));
		return credentialObj;
	}

	public double computeAmt(Member member,ArrayList<Individual> individuals) {
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

	public void index(HttpServletRequest req, HttpServletResponse resp) {
		ServletOutputStream out;
		try {
			out = resp.getOutputStream();
			out.print(getHeader());
			out.print(getBody());
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}

	public String getStyle() {
		StringBuilder sb = new StringBuilder();
		sb.append("html, body {height:100%;min-height:100vh;position:relative;background: linear-gradient(to bottom, #122b6b, #01b5ff 100%) no-repeat;margin: 0 auto;padding: 0;}");
		sb.append("header {min-height: 100px; min-width: 600px;max-width:800px;margin: 0px auto;background: #122b6b url('/images/logo.png') no-repeat scroll top center;}");
		sb.append("nav {min-height: 10px; min-width: 600px;max-width:800px;margin: 0px auto;background: blue;border: blue;border-radius: 18px 18px 18px 18px;}");
		sb.append("body {color:white;overflow hidden;}");
		sb.append(".registration {min-height: 100px; min-width: 600px;max-width:800px;margin: 0px auto;border: 1px solid yellow;border-radius: 18px 18px 0 0;}");
		sb.append(".registration table {width: 100%;}");
		sb.append(".registration tr:nth-child(odd){border: 2px solid yellow;}");
		sb.append(".receipt {border: 2px solid black;max-width: 800px; background-color: light blue}");
		sb.append(".memberCard {border: 2px solid red;max-width:600px;background-color: yellow;}");
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

	public String getHeader() {
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
		sb.append("<title>").append(poolName).append("</title>");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js\"></script>");
		sb.append(
				"<link rel=\"stylesheet\" href=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/themes/smoothness/jquery-ui.css\">");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js\"></script>");
		sb.append(" <meta charset=\"utf-8\"/>");
		sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\" />");
		sb.append("</head>");
		return sb.toString();
	}

	public String getBody() {
		StringBuilder sb = new StringBuilder();
		sb.append("<body onload=\"startup();\"/>");
		sb.append("<a href=\"http://www.wedgewoodhillsswimclub.com\"><header></header></a>");
		sb.append("<nav></nav>");
		sb.append("<div class=registration id=registration>");
		sb.append("<script>");
		sb.append("function rateInfo() {");
		sb.append("var rate = document.getElementById('rate');");
		sb.append("document.getElementById('rateinfo').innerHTML = \"\";");
		sb.append(
				"if(new String(rate.options[rate.selectedIndex].value).valueOf() == new String(\"family\").valueOf()) { document.getElementById('rateinfo').innerHTML = \"<p>Family:  A family living in the same househould with a max of 2 adults, and up to 4 childern.</p>\";}");
		sb.append(
				"if(new String(rate.options[rate.selectedIndex].value).valueOf() == new String(\"teen\").valueOf()) { document.getElementById('rateinfo').innerHTML = \"<p>Teen:  An individual between the ages of 12 to 18.  Teen members are not permitted to purchase guest passes for other teens.  Teens must have a membership or come in as a guest of an adult.</p>\";}");
		sb.append("}\r\n");
		sb.append("function addIndividual() {");
		sb.append("var i=0;");
		sb.append("var indname = document.getElementsByName(\"indname\"+i);");		
		sb.append("while(indname != null && indname.length > 0) {i++;indname = document.getElementsByName(\"indname\"+i);}");
		sb.append("var tblIndviduals = document.getElementById('tblIndividuals');");
		sb.append("var row = tblIndividuals.insertRow(-1);");
		sb.append("var cell1 = row.insertCell(0);");
		sb.append("var cell2 = row.insertCell(1);");
		sb.append("var cell3 = row.insertCell(2);");		
		sb.append("cell1.innerHTML = \"<input name=indname\" + i + \" type=text />\";");
		sb.append("cell2.innerHTML = \"")
		   .append("<select name=inddobmm\" + i + \" >")
		   .append("<option value=1 >Jan</option>")
		   .append("<option value=2 >Feb</option>")
		   .append("<option value=3 >Mar</option>")
		   .append("<option value=4 >Apr</option>")
		   .append("<option value=5 >May</option>")
		   .append("<option value=6 >Jun</option>")
		   .append("<option value=7 >Jul</option>")
		   .append("<option value=8 >Aug</option>")
		   .append("<option value=9 >Sep</option>")
		   .append("<option value=10 >Oct</option>")
		   .append("<option value=11 >Nov</option>")
		   .append("<option value=12 >Dec</option>").append("</select>")
		   .append("<select name=inddobdd\" + i + \">")
		   .append("<option value=1 >1</option>")
		   .append("<option value=2 >2</option>")
		   .append("<option value=3 >3</option>")
		   .append("<option value=4 >4</option>")
		   .append("<option value=5 >5</option>")
		   .append("<option value=6 >6</option>")
		   .append("<option value=7 >7</option>")
		   .append("<option value=8 >8</option>")
		   .append("<option value=9 >9</option>")
		   .append("<option value=10 >10</option>")
		   .append("<option value=11 >11</option>")
		   .append("<option value=12 >12</option>")
		   .append("<option value=13 >13</option>")
		   .append("<option value=14 >14</option>")
		   .append("<option value=15 >15</option>")
		   .append("<option value=16 >16</option>")
		   .append("<option value=17 >17</option>")
		   .append("<option value=18 >18</option>")
		   .append("<option value=19 >19</option>")
		   .append("<option value=20 >20</option>")
		   .append("<option value=21 >21</option>")
		   .append("<option value=22 >22</option>")
		   .append("<option value=23 >23</option>")
		   .append("<option value=24 >24</option>")
		   .append("<option value=25 >25</option>")
		   .append("<option value=26 >26</option>")
		   .append("<option value=27 >27</option>")
		   .append("<option value=28 >28</option>")
		   .append("<option value=29 >29</option>")
		   .append("<option value=30 >30</option>")
		   .append("<option value=31 >31</option>")
		   .append("</select>").append("<select name=inddobyyyy\" + i + \">");
		Calendar now = Calendar.getInstance();
		for(int yy = now.get(Calendar.YEAR);yy > (now.get(Calendar.YEAR)-100);yy--) {
			sb.append("<option value=").append(yy).append(">").append(yy).append("</option>");
		}
		sb.append("</select></td>");
		sb.append("cell3.innerHTML = \"<select name=indgender\" + i	+ \" >;")
		  .append("<option value=F >Female</option>")
		  .append("<option value=M >Male</option>")
		  .append("</select>\";");				
		sb.append("}\r\n");
		sb.append("</script>");
		sb.append("<form method=POST />");
		sb.append("<h2>Pool Registration</h2>");
		sb.append("<table id=tblIndividuals >");
		sb.append("<tbody><tr><td class=label>Name</td><td class=input><input type=text name=name /></td></tr>");
		sb.append("<tr><td class=label>Address</td><td class=input><input type=text name=address /></td></tr>");
		sb.append("<tr><td class=label>City State Zip</td><td class=input><input type=text name=csz /></td></tr>");
		sb.append("<tr><td class=label>Phone</td><td class=input><input type=text name=phone /></td></tr>");
		sb.append("<tr><td class=label>Email</td><td class=input><input type=text name=email /></td></tr>");
		sb.append("<tr><td class=label>&nbsp;</td><td class=input>");
		sb.append("<select name=rate onchange=\"rateInfo();\" oninput=\"rateInfo();\" >");
		sb.append("<option selected value=family>Family (Max 2 Adults, up to 4 childern)</option>");
		sb.append("<option value=babysitter>Baby Sitter</option>");
		sb.append("<option value=couple>Couple</option>");
		sb.append("<option value=single>Single</option>");
		sb.append("<option value=senior>Senior Single</option>");
		sb.append("<option value=seniorcouple>Senior Couple</option>");
		sb.append("<option value=teen>Teen</option>");
		sb.append("</select>");
		sb.append("</td></tr>");
		sb.append("<tr><td colspan=2>");
		sb.append(
				"<div id=rateinfo><p>Family:  A family living in the same househould with a max of 2 adults, and up to 4 childern.</p></div>");
		sb.append("</td></tr><tr><td colspan=2>");
		sb.append("<h3>Individuals</h3>");
		sb.append("</td></tr>");
		sb.append("<tr><th>Name</th><th>Date of Birth</th><th>Gender</th></tr>");
		for (int i = 0; i < 6; i++) {
			sb.append("<tr><td><input name=indname" + i + " type=text /></td>")
			   //.append("<td><input type=text name=inddob").append(i).append(" /></td>")
			   .append("<td><select name=inddobmm").append(i).append(" >")
			   .append("<option value=1 >Jan</option>")
			   .append("<option value=2 >Feb</option>")
			   .append("<option value=3 >Mar</option>")
			   .append("<option value=4 >Apr</option>")
			   .append("<option value=5 >May</option>")
			   .append("<option value=6 >Jun</option>")
			   .append("<option value=7 >Jul</option>")
			   .append("<option value=8 >Aug</option>")
			   .append("<option value=9 >Sep</option>")
			   .append("<option value=10 >Oct</option>")
			   .append("<option value=11 >Nov</option>")
			   .append("<option value=12 >Dec</option>").append("</select>")
			   .append("<select name=inddobdd").append(i).append(">")
			   .append("<option value=1 >1</option>")
			   .append("<option value=2 >2</option>")
			   .append("<option value=3 >3</option>")
			   .append("<option value=4 >4</option>")
			   .append("<option value=5 >5</option>")
			   .append("<option value=6 >6</option>")
			   .append("<option value=7 >7</option>")
			   .append("<option value=8 >8</option>")
			   .append("<option value=9 >9</option>")
			   .append("<option value=10 >10</option>")
			   .append("<option value=11 >11</option>")
			   .append("<option value=12 >12</option>")
			   .append("<option value=13 >13</option>")
			   .append("<option value=14 >14</option>")
			   .append("<option value=15 >15</option>")
			   .append("<option value=16 >16</option>")
			   .append("<option value=17 >17</option>")
			   .append("<option value=18 >18</option>")
			   .append("<option value=19 >19</option>")
			   .append("<option value=20 >20</option>")
			   .append("<option value=21 >21</option>")
			   .append("<option value=22 >22</option>")
			   .append("<option value=23 >23</option>")
			   .append("<option value=24 >24</option>")
			   .append("<option value=25 >25</option>")
			   .append("<option value=26 >26</option>")
			   .append("<option value=27 >27</option>")
			   .append("<option value=28 >28</option>")
			   .append("<option value=29 >29</option>")
			   .append("<option value=30 >30</option>")
			   .append("<option value=31 >31</option>")
			   .append("</select>").append("<select name=inddobyyyy").append(i).append(">");
			Calendar cal = Calendar.getInstance();
			for(int yy = cal.get(Calendar.YEAR);yy > (cal.get(Calendar.YEAR)-100);yy--) {
				sb.append("<option value=").append(yy).append(">").append(yy).append("</option>");
			}
			sb.append("</select></td>");
			sb.append("<td><select name=indgender").append(i).append("><option value=F >Female</option><option value=M >Male</option></select</td></tr>");
		}
		sb.append("</table>");
		sb.append("<input type=button value=\"Add Individual\" onclick=\"addIndividual();\" >");
		sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		sb.append("<input type=submit value=Submit />");
		sb.append("<input type=hidden name=action value=register />");
		sb.append("</form>");
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");
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
