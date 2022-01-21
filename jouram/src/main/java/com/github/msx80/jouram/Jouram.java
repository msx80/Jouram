package com.github.msx80.jouram;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.msx80.jouram.core.InstanceController;
import com.github.msx80.jouram.core.InstanceManager;
import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.JouramBuilder;
import com.github.msx80.jouram.core.Jouramed;
import com.github.msx80.jouram.core.fs.VFile;
import com.github.msx80.jouram.core.map.FrontFacingMap;
import com.github.msx80.jouram.core.map.JouramMap;
import com.github.msx80.jouram.core.map.JouramMapImpl;
import com.github.msx80.jouram.core.utils.SerializableSeder;
import com.github.msx80.jouram.core.utils.SerializationEngine;

/**
 * Persists an object by serializing its state and method calls in a journal.
 * Jouram is asyncronous: each method call is enqueued to the journal but returns immediately
 * eventual errors are propagated to the client as soon as possible in the subsequent calls.
 * you can use snapshot() to close the method call journal and write a fresh copy of the object
 * and sync() to make sure all backlog is written to disk.
 * 
 * Persisted objects must follow these rules:
 * * have all method that change its state marked by the @Mutator annotation
 * * be completely deterministic (no use of random, new Date() or currentTimeMillis etc. inside mutators)
 * * methods marked by @Mutator are allowed to throw Exceptions only if the leave the state unchanged.
 * * no modifiable state should be exposed (ie no getMap() returning internal Map that can be modified externally)
 * * all mutation must happen throu calling of Mutable methods on the interface.
 */
public final class Jouram {

	private static Set<String> openedDb = new HashSet<>();
	
	private Jouram() {
	}

	public static <E> JouramBuilder<E,E> setup(Class<E> yourInterface, E initiallyEmptyInterface)
	{
		return new JouramBuilder<E, E>(yourInterface, initiallyEmptyInterface, s -> {
			return Jouram.open(s.dbFolder, s.dbName, s.yourInterface, s.initialEmptyInstance, s.nullForDefault, s.async);
		});
	}
	
	/**
	 * Open a jouram database. You must supply the interface to be persisted
	 * and an initial instance. This initial instance will be used to create the
	 * database, only if it's not already there. If the DB already exists, this
	 * instance is discarded and the persisted one is used.
	 * The returned interface must be used anywhere to access the db, don't use direct
	 * handles and the updates made outside of the returned interface will be invisible.
	 * @param dbFolder the folder where the db files will be located
	 * @param dbName the name of the db. The name of all files created by jouram will start with this string
	 * @param yourInterface The interface of the object to persist
	 * @param initialEmptyInstance default empty initial instance, for first time use.
	 * @return The persisting interface. You have to use this handle to access your classes.
	 * @throws JouramException if anything go wrong.
	 */
	public static <E> E open(VFile dbFolder, String dbName, Class<E> yourInterface, E initialEmptyInstance, SerializationEngine nullForDefault, boolean async) throws JouramException
	{
		if(openedDb.contains(dbName))
		{
			throw new JouramException("A database with name '"+dbName+"' is already open. Close it first or use a different name.");
		}
		Objects.requireNonNull(dbFolder);
		Objects.requireNonNull(dbName);
		Objects.requireNonNull(yourInterface);
		Objects.requireNonNull(initialEmptyInstance);
		
		if(!yourInterface.isInterface()) throw new JouramException("yourInterface should be an Interface.");
		
		
		if (nullForDefault == null) nullForDefault = new SerializableSeder();
		InstanceManager m = new InstanceManager(nullForDefault, dbFolder, dbName);
		E e = m.open(yourInterface, initialEmptyInstance, async, () -> {
			Jouram.openedDb.remove(dbName);
		});
		openedDb.add(dbName);
		return e;
	}

	
	/**
	 * Close an instance of Jouram. Note that the instance proxy cannot be used anymore.
	 * Closing this way will make a snapshot if there are pending journal entries, to create
	 * a clean situation for next restart.
	 * @param instance the instance to close.
	 * @throws JouramException
	 */
	public static void close(Object instance) throws JouramException
	{
		asJouramed(instance).getJouram().commandClose(false);
	}
	/**
	 * Rapidly close an instance of Jouram. Note that the instance proxy cannot be used anymore.
	 * Closing this way will NOT make a snapshot if there are pending journal entries.
	 * Next restart will eventually find a journal and replay it.
	 * @param instance the instance to close.
	 * @throws JouramException
	 */
	public static void kill(Object instance) throws JouramException
	{
		asJouramed(instance).getJouram().commandClose(true);
	}
	
	/**
	 * Run the passed code in a Jouram transaction, that is: either all or none of the changes
	 * happening in the Runnable are persisted. 
	 * @param instance
	 * @param run
	 * @throws JouramException
	 */
	public static void transactional(Object instance, Runnable run) throws JouramException
	{
		InstanceController m = asJouramed(instance).getJouram();
		m.commandStartTransaction();
		try
		{
			run.run();
		}
		finally
		{
			m.commandEndTransaction();
		}
	}
	public static void startTransaction(Object instance) throws JouramException
	{
		asJouramed(instance).getJouram().commandStartTransaction();
	}
	public static void endTransaction(Object instance) throws JouramException
	{
		asJouramed(instance).getJouram().commandEndTransaction();
	}
	
	private static Jouramed asJouramed(Object instance) 
	{
		if(instance instanceof Jouramed)
		{
			return (Jouramed) instance;
		}
		else
		{
			throw new JouramException("Object is not managed by Jouram");
		}
	}

	/**
	 * Dumps the entire instance on the db file and removes the journal, obtaining a clean
	 * situation with no modifications pending. This is done automatically on opening and closing of the db, but can be
	 * called manually to avoid indefinite growth of the journal.
	 * Note that this method will wait for the snapshot to be completed before returning.
	 * @param instance The instance to snapshot
	 * @param minimalJournalEntry the number of entries required to do a snapshot, if less the method does nothing
	 * @throws JouramException
	 */
	public static void snapshot(Object instance, long minimalJournalEntry) throws JouramException
	{
		asJouramed(instance).getJouram().commandSnapshot(minimalJournalEntry);
	}
	
	/**
	 * Makes sure any pending modification is written on the disk.
	 * Wait for all enqueued messages to be processed, flushes it
	 * and return.
	 * @param instance The jouramed instance
	 * @throws JouramException
	 */
	public static void sync(Object instance) throws JouramException
	{
		asJouramed(instance).getJouram().commandSync();
	}
	
	/**
	 * Convenience method that returns a "read only" view of the instance,
	 * that is: an instance that will throw UnsupportedOperationException if
	 * a method marked with @Mutator is called.
	 * Useful when you want to pass the instance to some context you don't have
	 * full control. The underlying data is the same and calls are serialized with
	 * the same mutex as the main instance, ensuring proper serialization.
	 * @param <E>
	 * @param instance
	 * @return
	 * @throws JouramException
	 */
	@SuppressWarnings("unchecked")
	public static <E> E readOnly(E instance) throws JouramException
	{
		return (E) asJouramed(instance).getJouram().makeReadOnlyProxy();
	}

	/**
	 * Utility method that returns (a builder to) a persisted Map. 
	 * All jouram methods like close() etc will work on the returned object.
	 * @param <K>
	 * @param <V>
	 * @param keyClass
	 * @param valueClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> JouramBuilder<JouramMap<K, V>, Map<K,V>> map(Class<K> keyClass, Class<V> valueClass)
	{
		Class<JouramMap<K, V>> cls = (Class<JouramMap<K, V>>)(Object)JouramMap.class;
		
		JouramBuilder<JouramMap<K, V>, Map<K,V>> setup = new JouramBuilder<JouramMap<K, V>, Map<K, V>>(cls, new JouramMapImpl<K,V>(), s -> {

			JouramMap<K,V> a = (JouramMap<K, V>) Jouram.open(s.dbFolder, s.dbName, s.yourInterface, s.initialEmptyInstance, s.nullForDefault, s.async);
			return new FrontFacingMap<K,V>(a);
			
		});
		return setup;
	}

	
}
