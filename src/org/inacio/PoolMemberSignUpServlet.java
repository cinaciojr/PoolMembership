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
	static final String poolName = "Wedgewood Hills Swim Club";
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
			regInfo(req, resp); // Add new records to Member and Individuals and
								// calls subTotal
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
		// String keyStr = KeyFactory.keyToString(k);
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
			if (!ind.getName().trim().isEmpty()) {
				ind.setId(IndividualIO.add(ind));
				individuals.add(ind);
			}
		}
		member.setTotalAmt(Common.computeAmt(member, individuals));
		member.add();
		String htmlContent = subTotal(member, individuals);
		try (ServletOutputStream out = resp.getOutputStream()) {
			out.print(htmlContent);
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}

	public void subTotal(HttpServletRequest req, HttpServletResponse resp) {
		String id = req.getParameter("id");
		Member member = MemberIO.get(id);
		// String keyStr = KeyFactory.keyToString(member.getId());
		ArrayList<Individual> individuals = new ArrayList<Individual>();
		individuals = IndividualIO.getIndividualsOfMember(member.getId());
		String htmlContent = subTotal(member, individuals);
		try (ServletOutputStream out = resp.getOutputStream()) {
			out.print(htmlContent);
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}

	public String subTotal(Member member, ArrayList<Individual> individuals) {
		StringBuilder sb = new StringBuilder();
		sb.append(Common.getHeader());
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
						.append(Common.getAge(individuals.get(i).getDateOfBirth())).append("]</td></tr>");
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
		htmlContent.append(Common.getHeader());
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
		try (ServletOutputStream out = resp.getOutputStream()) {
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
		if (paypal != null && !paypal.equals(member.getTransactionId())) {
			member.setTransactionId(paypal);
			member.setDatePaid(Calendar.getInstance().getTimeInMillis());
			member.add();
		}
		sb.append(Common.getHeader());
		sb.append("<body><a href=\"http://www.wedgewoodhillsswimclub.com\"><header></header></a><nav></nav>");
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
		if (transId != null) {
			sb.append("<tr><td>&nbsp;</td><td>").append((transId)).append("</td></tr>");
		}
		sb.append("<tr><td>&nbsp;</td><td>Member Id: E-").append(id.substring(id.length() - 5)).append("</td></tr>");
		sb.append("</table></div>");
		Individual ind;
		Calendar now = Calendar.getInstance();
		for (int i = 0; i < individuals.size(); i++) {
			ind = individuals.get(i);
			sb.append("<div class=\"memberCard ").append(member.getRate().toUpperCase());
			if (Common.getAge(ind.getDateOfBirth()) < 18) {
				sb.append(" minor");
			}
			sb.append("\">");
			sb.append("<table>");
			sb.append("<tr><td colspan=2><img src=/images/logo.png /></td></tr>");
			sb.append("<tr><td><span style=\"text-decoration: normal;font-size: .8em;\">")
					.append(member.getRate().trim().toUpperCase()).append(": ").append(member.getName())
					.append("</span></td><td><span style=\"text-decoration: normal;font-size: .8em;\">Member Id: E-")
					.append(id.substring(id.length() - 5)).append("</span></td></tr>");
			sb.append("<tr><td colspan=2><span style=\"text-decoration: bold;font-size: 1.5em;\">Member: ")
					.append(ind.getName()).append("</span></td></tr>");
			sb.append("<tr><td colspan=2 align=center><span style=\"text-decoration: normal;font-size: .6em;\">")
					.append(poolName).append(" ").append(now.get(Calendar.YEAR)).append("  Season ")
					.append("</span></td></tr>");
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

	public void index(HttpServletRequest req, HttpServletResponse resp) {
		try (ServletOutputStream out = resp.getOutputStream()) {
			out.print(Common.getHeader());
			out.print(getBody());
			out.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}
	}

	public String getBody() {
		StringBuilder sb = new StringBuilder();
		sb.append("<body onload=\"startup();\"/>");
		sb.append("<a href=\"http://www.wedgewoodhillsswimclub.com\"><header></header></a>");
		sb.append("<nav></nav>");
		sb.append("<div class=registration id=registration>");
		sb.append("<script>\r\n");
		sb.append("function rateInfo() {");
		sb.append("var rate = document.getElementById('rate');");
		sb.append("document.getElementById('rateinfo').innerHTML = \"\";");
		sb.append("var rateSelected = new String(rate.options[rate.options.selectedIndex].value).valueOf();");
		sb.append("if(rateSelected == new String(\"family\").valueOf())").append(
				"{ document.getElementById('rateinfo').innerHTML = \"<p>Family:  A family living in the same househould with a max of 2 adults, and up to 4 children.</p>\";}");
		sb.append("if(rateSelected == new String(\"teen\").valueOf())").append(
				"{ document.getElementById('rateinfo').innerHTML = \"<p>Teen:  An individual between the ages of 12 to 18.  Teen members are not permitted to purchase guest passes for other teens.  Teens must have a membership or come in as a guest of an adult.</p>\";}");
		sb.append("document.getElementById('btnAddIndividual').disabled=false;");
		sb.append("}\r\n");
		sb.append("function addIndividual() {");
		sb.append("var i=0;");
		sb.append("var indname = document.getElementsByName(\"indname\"+i);");
		sb.append(
				"while(indname != null && indname.length > 0) {i++;indname = document.getElementsByName(\"indname\"+i);}");
		sb.append("var max = -1;");
		sb.append("var rateSelected = new String(rate.options[rate.selectedIndex].value).valueOf();");
		sb.append("if(rateSelected == new String(\"family\").valueOf()){max = -1;}");
		sb.append("if(rateSelected == new String(\"teen\").valueOf()){max = 1;}");
		sb.append("if(rateSelected == new String(\"single\").valueOf()){max = 1;}");
		sb.append("if(rateSelected == new String(\"couple\").valueOf()){max = 2;}");
		sb.append("if(rateSelected == new String(\"senior\").valueOf()){max = 1;}");
		sb.append("if(rateSelected == new String(\"seniorcouple\").valueOf()){max = 2;}");
		sb.append("if(rateSelected == new String(\"babysitter\").valueOf()){max = 1;}");
		sb.append(
				"if(i >= max && max != -1) {alert('Max number of individuals for this rate plan'); document.getElementById('btnAddIndividual').disabled=true; return;}");
		sb.append("var tblIndviduals = document.getElementById('tblIndividuals');");
		sb.append("var row = tblIndividuals.insertRow(-1);");
		sb.append("var cell1 = row.insertCell(0);");
		sb.append("var cell2 = row.insertCell(1);");
		sb.append("var cell3 = row.insertCell(2);");
		sb.append("cell1.innerHTML = \"<input name=indname\" + i + \" type=text />\";");
		sb.append("cell2.innerHTML = \"").append("<select name=inddobmm\" + i + \" >")
				.append("<option value=1 >Jan</option>").append("<option value=2 >Feb</option>")
				.append("<option value=3 >Mar</option>").append("<option value=4 >Apr</option>")
				.append("<option value=5 >May</option>").append("<option value=6 >Jun</option>")
				.append("<option value=7 >Jul</option>").append("<option value=8 >Aug</option>")
				.append("<option value=9 >Sep</option>").append("<option value=10 >Oct</option>")
				.append("<option value=11 >Nov</option>").append("<option value=12 >Dec</option>").append("</select>")
				.append("<select name=inddobdd\" + i + \">").append("<option value=1 >1</option>")
				.append("<option value=2 >2</option>").append("<option value=3 >3</option>")
				.append("<option value=4 >4</option>").append("<option value=5 >5</option>")
				.append("<option value=6 >6</option>").append("<option value=7 >7</option>")
				.append("<option value=8 >8</option>").append("<option value=9 >9</option>")
				.append("<option value=10 >10</option>").append("<option value=11 >11</option>")
				.append("<option value=12 >12</option>").append("<option value=13 >13</option>")
				.append("<option value=14 >14</option>").append("<option value=15 >15</option>")
				.append("<option value=16 >16</option>").append("<option value=17 >17</option>")
				.append("<option value=18 >18</option>").append("<option value=19 >19</option>")
				.append("<option value=20 >20</option>").append("<option value=21 >21</option>")
				.append("<option value=22 >22</option>").append("<option value=23 >23</option>")
				.append("<option value=24 >24</option>").append("<option value=25 >25</option>")
				.append("<option value=26 >26</option>").append("<option value=27 >27</option>")
				.append("<option value=28 >28</option>").append("<option value=29 >29</option>")
				.append("<option value=30 >30</option>").append("<option value=31 >31</option>").append("</select>")
				.append("<select name=inddobyyyy\" + i + \">");
		Calendar now = Calendar.getInstance();
		for (int yy = now.get(Calendar.YEAR); yy > (now.get(Calendar.YEAR) - 100); yy--) {
			sb.append("<option value=").append(yy).append(">").append(yy).append("</option>");
		}
		sb.append("</select>\";");
		sb.append("cell3.innerHTML = \"<select name=indgender\" + i	+ \" >;").append("<option value=F >Female</option>")
				.append("<option value=M >Male</option>").append("</select>\";");
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
		sb.append("<select id=rate name=rate onchange=\"rateInfo();\" oninput=\"rateInfo();\" >");
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
				"<div id=rateinfo><p>Family:  A family living in the same househould with a max of 2 adults, and up to 4 children.</p></div>");
		sb.append("</td></tr><tr><td colspan=2>");
		sb.append("<h3>Individuals</h3>");
		sb.append("</td></tr>");
		sb.append("<tr><th>Name</th><th>Date of Birth</th><th>Gender</th></tr>");
		for (int i = 0; i < 1; i++) {
			sb.append("<tr><td><input name=indname" + i + " type=text /></td>")
					// .append("<td><input type=text
					// name=inddob").append(i).append(" /></td>")
					.append("<td><select name=inddobmm").append(i).append(" >").append("<option value=1 >Jan</option>")
					.append("<option value=2 >Feb</option>").append("<option value=3 >Mar</option>")
					.append("<option value=4 >Apr</option>").append("<option value=5 >May</option>")
					.append("<option value=6 >Jun</option>").append("<option value=7 >Jul</option>")
					.append("<option value=8 >Aug</option>").append("<option value=9 >Sep</option>")
					.append("<option value=10 >Oct</option>").append("<option value=11 >Nov</option>")
					.append("<option value=12 >Dec</option>").append("</select>").append("<select name=inddobdd")
					.append(i).append(">").append("<option value=1 >1</option>").append("<option value=2 >2</option>")
					.append("<option value=3 >3</option>").append("<option value=4 >4</option>")
					.append("<option value=5 >5</option>").append("<option value=6 >6</option>")
					.append("<option value=7 >7</option>").append("<option value=8 >8</option>")
					.append("<option value=9 >9</option>").append("<option value=10 >10</option>")
					.append("<option value=11 >11</option>").append("<option value=12 >12</option>")
					.append("<option value=13 >13</option>").append("<option value=14 >14</option>")
					.append("<option value=15 >15</option>").append("<option value=16 >16</option>")
					.append("<option value=17 >17</option>").append("<option value=18 >18</option>")
					.append("<option value=19 >19</option>").append("<option value=20 >20</option>")
					.append("<option value=21 >21</option>").append("<option value=22 >22</option>")
					.append("<option value=23 >23</option>").append("<option value=24 >24</option>")
					.append("<option value=25 >25</option>").append("<option value=26 >26</option>")
					.append("<option value=27 >27</option>").append("<option value=28 >28</option>")
					.append("<option value=29 >29</option>").append("<option value=30 >30</option>")
					.append("<option value=31 >31</option>").append("</select>").append("<select name=inddobyyyy")
					.append(i).append(">");
			Calendar cal = Calendar.getInstance();
			for (int yy = cal.get(Calendar.YEAR); yy > (cal.get(Calendar.YEAR) - 100); yy--) {
				sb.append("<option value=").append(yy).append(">").append(yy).append("</option>");
			}
			sb.append("</select></td>");
			sb.append("<td><select name=indgender").append(i)
					.append("><option value=F >Female</option><option value=M >Male</option></select</td></tr>");
		}
		sb.append("</table>");
		sb.append("<input type=button value=\"Add Individual\" onclick=\"addIndividual();\" id=btnAddIndividual >");
		sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		sb.append("<input type=submit value=Submit />");
		sb.append("<input type=hidden name=action value=register />");
		sb.append("</form>");
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
}
