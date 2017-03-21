package org.inacio;

import com.google.appengine.api.datastore.Key;

public class Member {
	private Key id;
	private String name;
	private String address;
	private String csz;
	private String phone;
	private String email;
	private String rate;
	private double totalAmt;
	private long datePaid;
	private String transactionId;
	private String referral;
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
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the csz
	 */
	public String getCsz() {
		return csz;
	}
	/**
	 * @param csz the csz to set
	 */
	public void setCsz(String csz) {
		this.csz = csz;
	}
	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}
	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}
	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return the rate
	 */
	public String getRate() {
		return rate;
	}
	/**
	 * @param rate the rate to set
	 */
	public void setRate(String rate) {
		this.rate = rate;
	}
	/**
	 * @return the totalAmt
	 */
	public double getTotalAmt() {
		return totalAmt;
	}
	/**
	 * @param totalAmt the totalAmt to set
	 */
	public void setTotalAmt(double totalAmt) {
		this.totalAmt = totalAmt;
	}
	/**
	 * @return the datePaid
	 */
	public long getDatePaid() {
		return datePaid;
	}
	/**
	 * @param datePaid the datePaid to set
	 */
	public void setDatePaid(long datePaid) {
		this.datePaid = datePaid;
	}
	/**
	 * @return the transactionId
	 */
	public String getTransactionId() {
		return transactionId;
	}
	/**
	 * @param transactionId the transactionId to set
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	/**
	 * @return the referral
	 */
	public String getReferral() {
		return referral;
	}
	/**
	 * @param referral the referral to set
	 */
	public void setReferral(String referral) {
		this.referral = referral;
	}
	public Key add() {
		return MemberIO.add(this);	
	}
}
