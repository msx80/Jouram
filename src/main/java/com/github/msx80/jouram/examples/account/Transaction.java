package com.github.msx80.jouram.examples.account;

import java.io.Serializable;
import java.math.BigDecimal;


public class Transaction implements Serializable{

	private static final long serialVersionUID = 5182671084301238376L;

	private final BigDecimal amount;
	private final String reason;
	
	
	
	protected Transaction() {
		super();
		amount = null;
		reason = null;
	}

	public Transaction(BigDecimal amount, String reason) {
		super();
		this.amount = amount;
		this.reason = reason;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getReason() {
		return reason;
	}
	
	
	
	
}
