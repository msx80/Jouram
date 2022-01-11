package com.github.msx80.jouram.examples.account;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.msx80.jouram.core.utils.NonMutantIterator;

public class AccountImpl implements Account, Serializable 
{
	private static final long serialVersionUID = 1066711855812351810L;
	
	private String accountName;
	private List<Transaction> transactions = new ArrayList<Transaction>();
	
	
	
	protected AccountImpl() {
		super();
	}

	public AccountImpl(String accountName) {
		super();
		this.accountName = accountName;
	}

	@Override
	public void addMoney(BigDecimal howmuch, String description)
	{
		transactions.add(new Transaction(howmuch, description));
	}
	
	@Override
	public void removeMoney(BigDecimal howmuch, String description)
	{
		// it's ok to call another mutator method, just make sure this one is marked mutator as well! (in the interface)
		addMoney(howmuch.negate(), description);
	}
	
	@Override
	public BigDecimal balance()
	{
		BigDecimal tot = BigDecimal.ZERO;
		for (Transaction transaction : transactions) {
			tot = tot.add(transaction.getAmount());
		}
		
		return tot;
	}

	@Override
	public Iterator<Transaction> iterator() {
		
		// must be non mutant or we break the Jouram contract.
		// as Iterator has a remove optional method, make sure it will not work.
		// notice that calling a "remove" method of this class marked mutator would NOT work
		// as it will not pass throu the proxied interface.

		// note: it's ok to return internal Transaction objects only because
		// they're immutable
		return new NonMutantIterator<>(transactions.iterator());
	}

	@Override
	public String getAccountName() {
		return accountName;
	}

	@Override
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	
	
	
}
