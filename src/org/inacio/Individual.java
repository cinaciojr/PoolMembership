package org.inacio;

import java.util.Calendar;

import com.google.appengine.api.datastore.Key;

public class Individual {
	private Key id;
	private String name;
	private Key memberId;
	private long dob;
	private String gender;
	private boolean isAdult;
	private boolean isCollege;
	
	/**
	 * @return the id
	 */
	public Key getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Key id) {
		this.id = id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the memberId
	 */
	public Key getMemberId() {
		return memberId;
	}
	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(Key memberId) {
		this.memberId = memberId;
	}
	/**
	 * @return the dob
	 */
	public long getDob() {
		return dob;
	}
	public Calendar getDateOfBirth()
	{
		Calendar c = Calendar.getInstance();
		if(dob == -1)
		{
			c.setTimeInMillis(0l);
		} else {
			c.setTimeInMillis(dob);
		}
		return c;
	}
	/**
	 * @param dob the dob to set
	 */
	public void setDob(long dob) {
		this.dob = dob;
	}
	public void setDateOfBirth(Calendar c){
		if(c == null) {
			this.dob = -1;
		} else {
			this.dob = c.getTimeInMillis();
		}
	}
	/**
	 * @return the gender
	 */
	public String getGender() {
		return gender;
	}
	/**
	 * @param gender the gender to set
	 */
	public void setGender(String gender) {
		this.gender = gender;
	}
	/**
	 * @return the isAdult
	 */
	public boolean isAdult() {
		return isAdult;
	}
	/**
	 * @param isAdult the isAdult to set
	 */
	public void setAdult(boolean isAdult) {
		this.isAdult = isAdult;
	}
	/**
	 * @return the isCollege
	 */
	public boolean isCollege() {
		return isCollege;
	}
	/**
	 * @param isCollege the isCollege to set
	 */
	public void setCollege(boolean isCollege) {
		this.isCollege = isCollege;
	}
	
	public Key add() {
		return IndividualIO.add(this);
	}
}
