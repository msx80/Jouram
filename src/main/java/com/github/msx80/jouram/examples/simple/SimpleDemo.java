package com.github.msx80.jouram.examples.simple;

import java.nio.file.Paths;

import com.github.msx80.jouram.core.Jouram;

public class SimpleDemo {

	public static void main(String[] args) throws Exception 
	{
		// instantiate a Jouram engine, open database "demo" in current directory
		// and obtain our StringDb, we also pass our initial implementation that will
		// be used if the database is being created
		final StringDb db = Jouram.open(Paths.get("."), "demo", StringDb.class, new StringDbImpl());
		
		// do some work
		System.out.println("There are now "+db.size()+" entries.");
		db.add("Hello there");
		db.add("Time is: "+System.currentTimeMillis());
		db.add("Bye");
		System.out.println("There are now "+db.size()+" entries, here they are:");
		
		db.print();
		Jouram.sync(db);
		
		// try commenting the close and see how entries are restored and never lost.
		Jouram.close(db);
		System.exit(0);
	}
}
