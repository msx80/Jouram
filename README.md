[![Release](https://jitpack.io/v/msx80/jouram.svg)](https://jitpack.io/#msx80/jouram)
# Jouram #

A persistence system based on in-memory database and journaling, similar to prevalence systems.

### What is it? ###

It's an object persistence system that keeps all your objects in memory while providing a journalized database that persist your data and is fault tolerant. It's simple to integrate and to use. Just program your business object as you always do, and find them again the next time you run your program!

In a nutshell:

```java

// Have an interface with @Mutator tags on state mutating methods:
public interface Counter {
	int get();
	@Mutator void inc();
}

// Have your implementation:
public class CounterImpl() {
	int n;
	public int get(){ return n; }
	public void inc() { n++; } 
}

// Open your instance throu Jouram:
Counter myCounter = Jouram.setup(Counter.class, new CounterImpl()).open();

// now MyCounter is persisted!
myCounter.inc();
System.out.println(myCounter.get()); // will remember previous runs


```

### Limitations ###

There are a couple of things to keep in mind to use Jouram:

* All modifications to your persisted data must pass throu a single class/interface.
* Your business classes must have a deterministic behaviour (see more below).
* You business classes must be serializable with the serializer you choose to use (defaults to java Serializable engine, but a much more efficient Kryo engine is provided)
* All your business call will be "synchronized", ie executed serially, to grant that they do not overlap and are recorded in a meaningful way.

### How does it work ###

Jouram provide a transparent wrapper to your data interface (in the form of a proxy), where it record all the calls you make to your mutator methods. This call are journalized and can be replayed against your objects when the database needs to be restored. When you `close()` jouram (or at any time at your convenience by calling `snapshot()`), it will save the whole business object and reset the journal for a new start.
Jouram supports atomic transactions. You can group method calls into a transaction, so that, should the program be interrupted, either all or none of the method calls will be replayed.

### How to use ###

Using Jouram couldn't be easier: you just have to deal with a single class, `Jouram`, with few methods:
```java
public static <E> E open(...);
public static void close(...);
public static void snapshot(...);
...
```

So basically you just open Jouram, take snapshots (if and when you want) and finally close it. If your program is forcibly closed before snapshotting or closing, no data will be lost, all modifications will be recovered from the journal.

In details:

Define an interface to access your data. This must be your unique door to access and modify all data you want to persist. Mark mutator methods (methods that change your data rather than simply reading it) with the `Mutator` annotation.

```java

public interface StringDb {

	@Mutator
	public abstract void add(String s);
	@Mutator
	public abstract void remove(String s);

	public abstract void print();
	
}
```

Implement this interface as you prefer, keeping in mind that it must behave deterministically. Make sure you understand the implications of this.

Then you can use your class like this:

```java

public class SimpleDemo {

	public static void main(String[] args) throws Exception 
	{
		// instantiate a Jouram engine, open database "demo" in current directory
		// and obtain our StringDb, we also pass our initial implementation that will
		// be used if the database is being created
		StringDb db = Jouram.setup(StringDb.class, new StringDbImpl())
			.folder(Paths.get("."))
			.dbName("demo")
			.serializationEngine(new KryoSeder());
			.open();
		
		// do some work
		System.out.println("There are now "+db.size()+" entries.");
		db.add("Hello there");
		db.add("Time is: "+System.currentTimeMillis());
		db.add("Bye");
		System.out.println("There are now "+db.size()+" entries, here they are:");
		db.print();
		
		Jouram.close(db);
	}
}
```
### Sync vs Async ###

Jouram comes in two mode: sync and async. In sync mode, when you call a mutator method, all required journaling is done before returning control. If there's an error with the disk or something, you're immediately notified with an exception. In async mode, a call to the mutator method returns immediately after calling the delegate, and the actual journaling work is enqueued. This means much faster response time, at the cost that there's a small window where the data are only in RAM and a system crash could lose them. Also, in event of disk errors, the method has already returned succesfully. In this case the exception is thrown at the first occasion (like in the next method call). In async mode, you can call Jouram.sync() at any moment to ensure that all journal is written to disk.

### Determinism ###

Making a serie of classes that behaves with determinism is not as easy as it sounds like. The best thing to do is to simply store your data in arrays, collections etc. Here are some things that you have to avoid within your business classes:

* Third part services (http or other network requests, etc)
* Random sources (ie. Random class)
* IO access (disk read, IPC, etc)
* System clock ( System.currentTimeMillis() etc)

In most cases these limitations can be solved by making the call *before* calling your data, instead of inside your data, and then passing the relevant results as parameters to your objects.