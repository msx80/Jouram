package com.github.msx80.jouram.examples.account;

import java.math.BigDecimal;
import java.nio.file.Paths;

import com.github.msx80.jouram.Jouram;
import com.github.msx80.jouram.kryo.KryoSeder;

public class AccountDemo {
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty("java.util.logging.SimpleFormatter.format","[%1$tT:%1$tL] %4$.4s: %5$s [%2$s] %6$s %n");
		
		Account initial = new AccountImpl("My Bank Account");
		
		// passing an already modified initial instance is ok, as long as 
		// you then stop using the alias ("initial") and use the returned one ("a").
		// these modifications will be applied just once at db creation time
		initial.addMoney(BigDecimal.valueOf(2000), "Initial sum");
		
		Account a = Jouram
				.setup(Account.class, initial)
				.folder(Paths.get("."))
				.dbName("account")
				.serializationEngine(new KryoSeder())
				.async(true)
				.open();
		
		a.addMoney(BigDecimal.valueOf(1000), "Monthly pay");
		a.addMoney(BigDecimal.valueOf(40), "Tip from grandma");
		Jouram.snapshot(a,0);
		a.removeMoney(BigDecimal.valueOf(80), "New pair of shoes");
		a.removeMoney(BigDecimal.valueOf(500), "Mortgage");
		
		System.out.println(a.getAccountName());
		for (Transaction transaction : a) {
			System.out.println(transaction.getAmount()+"\t"+transaction.getReason());
		}
		System.out.println("Balance: "+a.balance());
		
		
		Jouram.close(a);

	}

}
