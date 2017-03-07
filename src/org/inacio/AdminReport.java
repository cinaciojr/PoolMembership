package org.inacio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class AdminReport
 */
public class AdminReport extends HttpServlet {
	private static final long serialVersionUID = 1L;
	final static Logger LOG = Logger.getLogger(AdminReport.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AdminReport() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String auth = request.getParameter("auth");
		if(auth.trim().equals("wwh2017")) {
			rptMembers(request,response);
		} else {
			response.sendError(403);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String auth = request.getParameter("auth");
		if(auth.trim().equals("wwh2017")) {
			rptMembers(request,response);
		} else {
			response.sendError(403);
		}
	}
	
	public void rptMembers(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String id = request.getParameter("id");
		StringBuilder htmlContent = new StringBuilder();
		ArrayList<Member> members = new ArrayList<Member>();
		if(id == null){
			members = MemberIO.getAllMembers();
		} else {
			Member member = MemberIO.get(id);
			members.add(member);
		}
		for(Member member:members) {
			ArrayList<Individual> individuals = IndividualIO.getIndividualsOfMember(member.getId());
			htmlContent.append(Common.getHeader());
			htmlContent.append("<body><header></header><nav></nav><div class=registration>");
			htmlContent.append("<table border=1><tr>");
			htmlContent.append("<th>").append(member.getName()).append("</th><th>").append(member.getAddress()).append("<br/>").append(member.getCsz())
			  .append("</th><th>").append(member.getRate()).append("</th></tr>");
			for(Individual i:individuals) {
				htmlContent.append("<tr><td>&nbsp;</td><td>").append(i.getName()).append("</td><td>").append(Common.getAge(i.getDateOfBirth())).append("</td><td>").append(i.getGender()).append("</td></tr>");
			}
		}
		htmlContent.append("</div></html>");
		try (ServletOutputStream out = response.getOutputStream()){
			out.print(htmlContent.toString());
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage());
		}		
	}

}
