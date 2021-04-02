package com.github.msx80.jouram.core;

import java.nio.file.Path;
import java.util.Objects;

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
public class Jouram {

	private Jouram() {
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
	public static <E> E open(Path dbFolder, String dbName, Class<E> yourInterface, E initialEmptyInstance, SerializationEngine nullForDefault) throws JouramException
	{
		Objects.requireNonNull(dbFolder);
		Objects.requireNonNull(dbName);
		Objects.requireNonNull(yourInterface);
		Objects.requireNonNull(initialEmptyInstance);
		if(!yourInterface.isInterface()) throw new JouramException("yourInterface should be an Interface.");
		
		
		if (nullForDefault == null) nullForDefault = new SerializableSeder();
		InstanceManager m = new InstanceManager(nullForDefault, dbFolder, dbName);
		return m.open(yourInterface, initialEmptyInstance);
	}

	public static <E> E open(Path dbFolder, String dbName, Class<E> yourInterface, E initialEmptyInstance) throws JouramException
	{
		return open(dbFolder, dbName, yourInterface, initialEmptyInstance, null);
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
		Jouramed j = asJouramed(instance);
		InstanceManager m = j.getJouram();
		m.commandClose();
	}
	
	public static void transactional(Object instance, Runnable run) throws JouramException
	{
		Jouramed j = asJouramed(instance);
		InstanceManager m = j.getJouram();
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
		Jouramed j = asJouramed(instance);
		InstanceManager m = j.getJouram();
		m.commandStartTransaction();
	}
	public static void endTransaction(Object instance) throws JouramException
	{
		Jouramed j = asJouramed(instance);
		InstanceManager m = j.getJouram();
		m.commandEndTransaction();
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
	 * Note that this does not wait for the snapshot to be completed, you can call sync() after this to make sure it's done.
	 * @param instance The instance to snapshot
	 * @param minimalJournalEntry the number of entries required to do a snapshot, if less the method does nothing
	 * @throws JouramException
	 */
	public static void snapshot(Object instance, long minimalJournalEntry) throws JouramException
	{
		Jouramed j = asJouramed(instance);
		InstanceManager m = j.getJouram();
		m.commandSnapshot(minimalJournalEntry);
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
		Jouramed j = asJouramed(instance);
		InstanceManager m = j.getJouram();
		m.commandSync();
	}
	
}
