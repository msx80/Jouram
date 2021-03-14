package com.github.msx80.jouram.examples.stress;

import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.Level;

import com.github.msx80.jouram.core.Jouram;
import com.github.msx80.jouram.kryo.KryoSeder;

public class StressMain {

	public static void main(String[] args) throws InterruptedException {
		System.setProperty("java.util.logging.SimpleFormatter.format","[%1$tT:%1$tL] %4$.4s: %5$s [%2$s] %6$s %n");
		
		final Database db = Jouram.open(Paths.get("."), "stressed", Database.class, new DatabaseImpl(), new KryoSeder());

		//http://ruedigermoeller.github.io/fast-serialization/
		
		System.out.println(db.count());
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				for (int i = 0; i < 100000; i++) {
					
					db.addMessage(new Date(), "Hello from thread "+Thread.currentThread().getName());
				}
				Jouram.sync(db);
				for (int i = 0; i < 100000; i++) {
					
					db.addMessage(new Date(), "Hello from thread "+Thread.currentThread().getName());
				}
				Jouram.sync(db);
				
			}
		};
		
		Thread[] t = new Thread[]{
			new Thread(r, "First"),
			new Thread(r, "Second"),
			new Thread(r, "Third"),
			new Thread(r, "Fourth"),
			new Thread(r, "Fifth")
		};
		for (Thread thread : t) {
			thread.start();
		}
		for (Thread thread : t) {
			thread.join();
		}
		System.out.println(db.count());
		//System.exit(0);
		Jouram.close(db);		
	}

}
