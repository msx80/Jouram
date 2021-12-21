package com.github.msx80.jouram.examples.stress;

import java.nio.file.Paths;
import java.util.Date;

import com.github.msx80.jouram.core.Jouram;
import com.github.msx80.jouram.kryo.KryoSeder;

public class StressMain {

	private static final int HOWMANY = 1000000;

	public static void main(String[] args) throws InterruptedException {
		
		final Database db = Jouram.open(Paths.get("."), "stressed", Database.class, new DatabaseImpl(), false, new KryoSeder());

		//http://ruedigermoeller.github.io/fast-serialization/
		
		System.out.println(db.count());
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				for (int i = 0; i < HOWMANY; i++) {
					
					db.addMessage(new Date(), "Hello from thread "+Thread.currentThread().getName());
				}
				Jouram.sync(db);
				for (int i = 0; i < HOWMANY; i++) {
					
					db.addMessage(new Date(), "Hello from thread "+Thread.currentThread().getName());
				}
				Jouram.sync(db);				
				for (int i = 0; i < HOWMANY; i++) {
					
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
			new Thread(r, "Fifth"),
			new Thread(r, "Sixth")
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
