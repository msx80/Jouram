[![Release](https://jitpack.io/v/msx80/jouram.svg)](https://jitpack.io/#msx80/jouram)
# Jouram #

A persistence system based on in-memory database and journaling, similar to prevalence systems.

### What is it? ###

It's an object persistence system that keeps all your objects in memory while providing a journalized database that persist your data and is fault tolerant. It's simple to integrate and to use. Just program your business object as you always do, and find them again the next time you run your program!

### Limitations ###

There are a couple of things to keep in mind to use Jouram:

* All modifications to your persisted data must pass throu a single class/interface.
* Your business classes must have a deterministic behaviour (see more below).
* You business classes must be serializable with the serializer you choose to use (defaults to java Serializable engine, but a much more efficient Kryo engine is provided)
* All your business call will be "synchronized", ie executed serially, to grant that they do not overlap and are recorded in a meaningful way.
* Exceptions raising from mutator methods are expected to leave the object unchanged.

### How does it work ###

Jouram provide a transparent wrapper to your data interface (in the form of a proxy), where it record all the calls you make to your mutator methods. This call are journalized and can be replayed against your objects when the database needs to be restored. When you `close()` jouram (or at any time at your convenience by calling `snapshot()`), it will save the whole business object and reset the journal for a new start.
Jouram supports atomic transactions. You can group method calls into a transaction, so that, should the program be interrupted, either all or none of the method calls will be replayed.

### How to use ###

Using Jouram couldn't be easier: you just have to deal with a single class, `Jouram`, with few methods:
```java
public static <E> E open(Path dbFolder, String dbName, Class<E> yourInterface, E initialEmptyInstance);
public static void close(Object instance);
public static void snapshot(Object instance);
...
```

So basically you just open Jouram, take snapshots (if and when you want) and finally close it. If your program is forcibly closed before snapshotting or closing, no data will be lost, all modifications will be recovered from the journal.

In details:

Define an interface to access your data. This must be your unique door to access and modify all data you want to persist. Mark mutator methods (methods that change your data rather than simply reading it) with the `Mutator` annotation.

```java
package jouram.examples.simple;

import jouram.core.Mutator;

/**
 * A very simple database of strings.
 * Support adding and removing of strings, as well as printing them and checking the size.
 * 
 */
public interface StringDb {

	/**
	 * Add the specified string to the database
	 * @param s
	 */
	@Mutator
	public abstract void add(String s);

	/**
	 * Remove the specified string from the database
	 * @param s
	 */
	@Mutator
	public abstract void remove(String s);

	/**
	 * Return the number of strings currently stored in the database
	 * @return
	 */
	public abstract int size();
	
	/**
	 * Prints the entire content of the database.
	 */
	public abstract void print();
	
}
```

Implement this interface as you prefer, keeping in mind that it must behave deterministically. Make sure you understand the implications of this.

Then you can use your class like this:

```java
package jouram.examples.simple;

import java.nio.file.Paths;

import jouram.core.Jouram;

public class SimpleDemo {

	public static void main(String[] args) throws Exception 
	{
		// instantiate a Jouram engine, open database "demo" in current directory
		// and obtain our StringDb, we also pass our initial implementation that will
		// be used if the database is being created
		StringDb db = Jouram.open(Paths.get("."), "demo", StringDb.class, new StringDbImpl());
		
		// do some work
		System.out.println("There are now "+db.size()+" entries.");
		db.add("Hello there");
		db.add("Time is: "+System.currentTimeMillis());
		db.add("Bye");
		System.out.println("There are now "+db.size()+" entries, here they are:");
		db.print();
		
		// try commenting the close and see how entries are restored and never lost.
		Jouram.close(db);
	}
}
```

### Determinism ###

Making a serie of classes that behaves with determinism is not as easy as it sounds like. The best thing to do is to simply store your data in arrays, collections etc. Here are some things that you have to avoid within your business classes:

* Third part services (http or other network requests, etc)
* Random sources (ie. Random class)
* IO access (disk read, IPC, etc)
* System clock ( System.currentTimeMillis() etc)

In most cases these limitations can be solved by making the call *before* calling your data, instead of inside your data, and then passing the relevant results as parameters to your objects.