package com.github.msx80.jouram.examples.account;

import java.math.BigDecimal;

import com.github.msx80.jouram.core.Mutator;

public interface Account extends Iterable<Transaction>{

	@Mutator
	void addMoney(BigDecimal howmuch, String description);

	@Mutator
	void removeMoney(BigDecimal howmuch, String description);

	BigDecimal balance();
	
	String getAccountName();
	
	@Mutator
	void setAccountName(String accountName);
}