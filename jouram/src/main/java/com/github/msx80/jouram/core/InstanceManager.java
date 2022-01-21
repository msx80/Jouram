package com.github.msx80.jouram.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.async.AsyncBridge;
import com.github.msx80.jouram.core.fs.VFile;
import com.github.msx80.jouram.core.sync.SyncBridge;
import com.github.msx80.jouram.core.utils.Deserializer;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Util;

public final class InstanceManager implements InvocationHandler, InstanceController {
	
	private static Logger LOG = LoggerFactory.getLogger(InstanceManager.class);
	private String tag;
	
	private Object instance = null;
	
	private DbVersion currentDbVersion;
	
	private Journal journal = null;
	
	public final VersionManager manager;
	
	boolean closed = false;
	Exception closedByException = null;

	private ClassData data;
	private final SerializationEngine seder;

	protected EngineBridge eng;
	
	/** 
	 * explicit lock to access Jouram. It's called on all client-exposed methods, they'll all run sequentially
	 * the worker thread instead will not use it.
	 * For mutator method calls, the lock will be used to do the call, while the actual journal log will be asynchronous
	 * on the worker thread. This will make calls fast and the actual IO will complete in background.
	 * 
	 * Snapshot will keep the lock for the whole operation so no concurrent mutator can happen. All enqueued mutator calls will be
	 * completed before the snapshot actually take place.
	 */
	private Object lock = new Object();
	private Class<?> yourInterface;
	private Runnable onClosed;

	public InstanceManager(SerializationEngine seder, VFile dbFolder, String dbName) {
		tag = "["+dbName+"] ";
		LOG.info(tag+"Creating Jouram '"+dbName+"' in '"+dbFolder.printableComplete()+"'. SerializationEngine: "+seder.getClass().getCanonicalName());
		manager = new VersionManager(dbFolder, dbName);
		this.seder = seder;
		
		journal = new JournalImpl(seder, manager);
	}
		
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
	{
		if(method.getDeclaringClass().equals(Jouramed.class))
		{
			// to get back our object
			assert method.getName().equals("getJouram");
			return this;
		}
		else
		{
			return callDelegate(method, args);
		}
    }

	private Object callDelegate(Method method, Object[] args) throws Exception {

		
		// need to sincronize the instance: all calls will be serialized one after the other with no overlapping.
		// this is necessary, even if the actual object is not synchronized, because the log file is one and each
		// call must be written when it's performed and finished.
		// non-mutator methods are synchronized too, because some call might be pending in the queue
		synchronized (lock) {
			
			// fail as soon as possible if the worker encountered an error writing something previously.
			checkClosedOrExcepted();
			
			// do the actual stuff BEFORE logging. If i do it after, if it throws exception the al would be unrecoverable
			// becouse recalling the method will launch the exception once again
			Exception tr = null;
	    	Object res = null;
			try {
				res = method.invoke(instance, args);
			} catch (Error e) {
				// If an Error is thrown, better not to even log as there could be all kinds of problems, like OutOfMemory etc. which are
				// definitely not deterministic. The journal replay would most likely do something different and break.
				throw (Error)e;
			}
			catch (InvocationTargetException e) {
				
				// get the naked exception out to the caller otherwise we change the logic of the interface.
				if(e.getTargetException() instanceof Exception) 
				{
					// if original exception is a regular exception, we use them to relaunch later after journaling
					tr = (Exception) e.getTargetException();
				}
				else
				{
					// if it's not, just throw it. It's probably an Error so as before we should not attempt to journal the method call.
					throw e;
				}

			}
		
			// is a mutator method?
			String id = data.getIdByMethod(method);
	    	if(id != null)
	    	{
	    		commandCallMethod(id, args, tr != null);
	    	}
	    	
	    	// now if the method was throwing an exception, we throw it
	    	if(tr!=null) throw tr;
	    	return res;
	    	
		}

	}

	private void checkClosedOrExcepted() {
		if(closedByException!=null) throw new JouramException("Jouram previously encountered an exception", closedByException); 	
		if(closed) throw new JouramException("Jouram is closed."); // no need to test for open, must be open to reach here
	}

	private void commandCallMethod(String id, Object[] args, boolean withException) 
	{
		eng.commandCallMethod(id, args, withException);
	}

	@Override
	public void commandSync()
	{
		synchronized(lock) 
		{
			checkClosedOrExcepted();
			eng.commandSync();
		}
	}
	
	@Override
	public void commandStartTransaction()
	{
		synchronized(lock) 
		{
			checkClosedOrExcepted();
			eng.commandStartTransaction();
		}
	}
	
	@Override
	public void commandEndTransaction()
	{
		synchronized(lock) 
		{
			checkClosedOrExcepted();
			eng.commandEndTransaction();
		}
	}
	
	public void doLogMethod(MethodCall mc, boolean shouldFlush) {
		checkWorkerThread();
		//  if so, journal this call
		LOG.debug(tag+"Journaling method {}", mc.methodId);
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeJournal(mc, shouldFlush);
	}
	
	public void doLogStartTransaction() {
		checkWorkerThread();
		//  if so, journal this call
		LOG.info(tag+"Journaling start transaction");
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeStartTransaction();
	}
	
	public void doLogEndTransaction(boolean shouldFlush) {
		checkWorkerThread();
		//  if so, journal this call
		LOG.info(tag+"Journaling end transaction");
		
		if(!journal.isInTransaction()) throw new JouramException("Not in a transaction");
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeEndTransaction(shouldFlush);
	}
	
		
	private void checkWorkerThread() {
		if(eng!=null) eng.checkWorkerThread();
	}

	public <E> E open(Class<E> yourInterface, E initialEmptyInstance, boolean async, Runnable onClosed) throws JouramException
	{
		try {
			return doOpen(yourInterface, initialEmptyInstance, async, onClosed);
		
		} catch (Exception e) {
			LOG.error(tag+"Error opening database: "+e.getMessage(), e);
			throw new JouramException("Error opening database: "+e.getMessage(),e);
		}
	}

	
	
	@SuppressWarnings("unchecked")
	private <E> E doOpen(Class<E> yourInterface, E defaultInstance, boolean async, Runnable onClosed) throws IOException, Exception {
		LOG.info(tag+"Opening Jouram");
		if(closed || (instance != null)) throw new JouramException("Jouram was already opened");
		
		this.yourInterface = yourInterface;
				
		currentDbVersion = manager.restorePreviousState();
		LOG.debug(tag+"Valid version identified: "+currentDbVersion);
		
		VFile dbMainFile = manager.getPathForDbFile(currentDbVersion);
		VFile dbJournal = manager.getPathForJournal(currentDbVersion);
		
		LOG.debug(tag+"Using version {} on files {}, {} ", currentDbVersion, dbMainFile, dbJournal);
		
		if(dbMainFile.exists())
		{
			long start = System.currentTimeMillis();
			LOG.info(tag+"Loading instance...");
			instance = (E)Util.objectFromFile(seder, dbMainFile, defaultInstance.getClass());
			LOG.info(tag+"Instance loaded in "+(System.currentTimeMillis()-start)+" millis.");
		}
		else
		{
			// new, never before seen db
			instance = defaultInstance;
			
			// must make sure the database file exists before the journal file
			try
			{
				saveInstance(currentDbVersion);
			}
			catch(Exception e)
			{
				// but if anything goes wrong, ensure no unreliable db file exists,
				// as it could break the first load
				dbMainFile.delete();
				throw e;
			}
		}

		// create the proxy. I also add the Jouramed interface to the list of implemented interfaces,
		// so i can obtain this InstanceManager with just a reference to the proxy.
		data = new ClassData(yourInterface);
		ClassLoader c = instance.getClass().getClassLoader();
		E myproxy = (E) Proxy.newProxyInstance(c, new Class[] { yourInterface, Jouramed.class }, this );

		
		if( handleJournal(dbJournal) )
		{
			// if there was journal to parse, better commit.
			// this way we can start with a fresh journal and avoid appending
			// which will be unsecure as we cannot be sure the last journal entry
			// is written entirely
			doSnapshot(0);
		}
		if(async)
			eng = new AsyncBridge(tag, this, this::markExcepted);
		else
			eng = new SyncBridge(this, this::markExcepted);
		
		this.onClosed = onClosed;
		return (E)myproxy;
	}

	private void markExcepted(Exception e)
	{
		//synchronized(lock) 
		{
			if(this.closedByException == null)
			{
				this.closedByException = e;
				onClosed.run();
			}
		}
	}
	
	private boolean handleJournal(VFile dbJournal) throws Exception {
		
		if(dbJournal.exists())
		{
			
			LOG.info(tag+"Replaying journal...");
			
			replayJournal(dbJournal);
			return true;
		}
		return false;
	}
	/*
	per implementare transazioni:
		mettere un byte di "tipo log" in journal. Tipo log puo' essere 
		-methodcall
		-start transaction
		-end transaction
		
		nel journal replay, quando arriva start transaction, salvare a parte tutte le successive methodcall e applicarle
		solo quando si raggiunge end transaction
	*/
	private void replayJournal(VFile dbJournal) throws Exception {
		long start = System.currentTimeMillis();
		int i = 0;
		int transactions = 0;
		List<MethodCall> transaction = new ArrayList<>();
		try {
			try(InputStream fis = dbJournal.read())
			{
				Deserializer d = seder.deserializer(fis);
				boolean finishedNaturally = false;
				while(!finishedNaturally)
				{
					int cmd = d.readByte();
					
					switch (cmd) {
					
					case JournalImpl.WRITE_LOG:
					{
						String methodId = d.read(String.class);
						Object[] parameters = d.read(Object[].class);
						LOG.trace("{} replay: log {}", tag, methodId);
						
						if(transactions == 0)
						{
							// exec right now
							Method m = data.getMethodById(methodId);
							m.invoke(instance, parameters);
							i++;
						}
						else
						{
							// add to transaction block
							transaction.add(new MethodCall(methodId, parameters, false));
						}
					}
						break;
					case JournalImpl.WRITE_LOG_EXCEPTION:
					{
						String methodId = d.read(String.class);
						Object[] parameters = d.read(Object[].class);
						LOG.trace("{} replay: log {}", tag, methodId);
						
						if(transactions == 0)
						{
							// exec right now
							Method m = data.getMethodById(methodId);
							boolean ex = false;
							try {
								m.invoke(instance, parameters);
							} catch (Exception e) {
								ex = true;
							}
							if(!ex) throw new JouramException("Method should have thrown an exception but didn't "+methodId);
							i++;
						}
						else
						{
							// add to transaction block
							transaction.add(new MethodCall(methodId, parameters, true));
						}
					}
						break;
						
					case JournalImpl.WRITE_START_TRANSACTION:
						LOG.trace("{} replay: start transaction", tag);
						transactions++;
						break;
						
					case JournalImpl.WRITE_END_TRANSACTION:
						LOG.trace("{} replay: end transaction", tag);
						transactions--;
						if(transactions == 0)
						{
							// transaction was originally closed, flush it
							for (MethodCall mc2 : transaction) {
								Method m = data.getMethodById(mc2.methodId);
								boolean ex = false;
								try {
									m.invoke(instance, mc2.parameters);
								} catch (Exception e) {
									ex = true;
								}
								if(ex!=mc2.withException)
								{
									new JouramException("Method should have thrown an exception but didn't, or vice versa. "+mc2.methodId);
								}
								i++;
							}
							transaction.clear();
						}
						break;
						
					case -1:
						// throw new EOFException("End of stream reached");
						finishedNaturally = true;
						break;
					default:
					 	throw new RuntimeException("Unexpected cmd "+cmd);
					}
				}
			}
		
			
		} catch (EOFException e) {
			// ok, reached the end of file
			LOG.warn(tag+"Journal truncated, last call(s) might not have been saved..");
		
		} catch (Exception e) {
			throw new JouramException("Unexpected error replaying journal", e);
		}
		
		LOG.info(tag+"Replayed "+i+" calls in "+(System.currentTimeMillis()-start)+" millis.");
		if(!transaction.isEmpty())
		{
			LOG.warn(tag+transaction.size()+" calls discarded becouse they were inside an uncompleted transaction.");	
		}
	}
	

	@Override
	public void commandSnapshot(long minimalJournalEntry) throws JouramException
	{
		synchronized(lock) 
		{
			checkClosedOrExcepted();

			// exit asap if journal is too short.
			// note: there may be calls in the queue to process but we're not counting them here
			// shouldn't make much difference and we can bug out much quicker
			if(journal.size() < minimalJournalEntry)
			{
				LOG.info(tag+"Not enought entries, no snapshot");
				return;
			}
			eng.commandSnapshot(minimalJournalEntry);
		}
	}
	

	public void doSync() {
		checkWorkerThread();
		long start = System.currentTimeMillis();
		LOG.info(tag+"Syncing");
		
		journal.flush();
		LOG.info(tag+"Synced in "+(System.currentTimeMillis()-start)+" millis.");
	}

	
	public void doSnapshot(long minimalJournalEntry) throws JouramException
	{
		
		// doesn't need to be synchronized as it is called in the worker thread sequentially
		// IT DOES instead, becouse the object can be called concurrently. A thread could call snapshot while
		// another could call a mutator method. Since callDelegate is synchronized, if this is synchronized too we
		// avoid concurrency issues.
		// update: synchronization moved upstream to commandSnapshot
		
		
			checkWorkerThread();
			long start = System.currentTimeMillis();
			LOG.info(tag+"Saving snapshot");
			
			if(!manager.getPathForJournal(currentDbVersion).exists())
			{
				// if we have no journal we don't need to save.
				// it would even be dangerous as we could end up with two db files and no journal. 
				LOG.info(tag+"No journal, no need to snapshot");
				return;
			}
			
			if(journal.size() < minimalJournalEntry)
			{
				LOG.info(tag+"Not enought entries, no snapshot");
				return;
			}
			
			if(journal.isInTransaction())
			{
				LOG.info(tag+"There is an active transaction, no snapshot");
				return;
			}
			
			try
			{
				journal.close();
				
				DbVersion old = currentDbVersion;
				DbVersion nev = currentDbVersion.next();
				
				// step 1
				saveInstance(nev);
				// step 2
				manager.deleteDb(old); // if exists
				
				currentDbVersion = nev; // at this point the changes are committed.
				
				// step 3
				manager.deleteJournal(old);
				
				LOG.info(tag+"Snapshot saved succesfully in "+(System.currentTimeMillis()-start)+" millis.");
			}
			catch(JouramException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				LOG.error(tag+"Error closing Jouram: "+e.getMessage(), e);
				throw new JouramException("Error closing Jouram: "+e.getMessage(), e);
			}
		
	}

	private void saveInstance(DbVersion version) throws Exception 
	{
		long start = System.currentTimeMillis();
		LOG.info("Saving instance...");
		Util.objectToFile(seder, manager.getPathForDbFile(version), instance);
		LOG.info("Instance saved in "+(System.currentTimeMillis()-start)+" millis.");
	}

	@Override
	public void commandClose(boolean quick) throws JouramException {
		synchronized(lock) {
			if (closed || (closedByException != null)) {
				return;
			}
			eng.commandClose(quick);
		}
	}

	public void doClose(boolean quick) throws JouramException {
		// doesn't need to be synchronized as it is called in the worker thread sequentially
		checkWorkerThread();
		
		if(closed)
		{
			// Already closed, returning..
		}
		else
		{
			LOG.info(tag+"Closing Jouram");
			closed = true;
			
			if(quick)
			{
				journal.close();
			}
			else
			{
				doSnapshot(0); // this also closes the journal
			}
			instance = null;
			data = null;
			
			onClosed.run();
			LOG.info(tag+"Jouram closed, bye");
		}
	
	}

	public Object makeReadOnlyProxy() {
		ClassLoader c = instance.getClass().getClassLoader();
		return Proxy.newProxyInstance(c, new Class[] { yourInterface }, new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if(method.isAnnotationPresent(Mutator.class))
				{
					throw new UnsupportedOperationException("Calling a mutator method on a read only Jouram proxy");
				}

				// still synchronize accesses
				synchronized (lock) {
					try {
						return method.invoke(instance, args);
					} catch (InvocationTargetException e) 
					{
						// get the naked exception out to the caller otherwise we change the logic of the interface.
						if(e.getTargetException() instanceof Exception) throw (Exception)e.getTargetException();
						else throw e; // if it's some other problem, throw it
					}
				}
			}
		} );
	}
	
}
