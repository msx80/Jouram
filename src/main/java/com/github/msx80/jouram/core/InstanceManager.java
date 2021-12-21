package com.github.msx80.jouram.core;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.queue.Cmd;
import com.github.msx80.jouram.core.queue.CmdClose;
import com.github.msx80.jouram.core.queue.CmdEndTransaction;
import com.github.msx80.jouram.core.queue.CmdMethodCall;
import com.github.msx80.jouram.core.queue.CmdSnapshot;
import com.github.msx80.jouram.core.queue.CmdStartTransaction;
import com.github.msx80.jouram.core.queue.CmdSync;
import com.github.msx80.jouram.core.utils.Deserializer;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Util;


class IntHolder {
    public int value;
}

class InstanceManager implements InvocationHandler {
	
	private static Logger LOG = LoggerFactory.getLogger(InstanceManager.class);
	private String tag;
	
	private Object instance = null;
	
	private DbVersion currentDbVersion;
	
	private Journal journal = null;
	
	private final VersionManager manager;
	
	boolean closed = false;

	private ClassData data;
	private final SerializationEngine seder;

	
	private JouramWorkerThread worker = null;
	
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
	

	public InstanceManager(SerializationEngine seder, Path dbFolder, String dbName, boolean autoSync) {
		tag = "["+dbName+"] ";
		LOG.info(tag+"Creating Jouram '"+dbName+"' in "+dbFolder.toAbsolutePath()+ " autoSync:"+autoSync);
		manager = new VersionManager(dbFolder, dbName);
		this.seder = seder;
		
		journal = new JournalImpl(seder, manager, autoSync);
	}
		
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
			if(worker.exception != null) throw new JouramException("Jouram previously encountered an exception", worker.exception);
			
				
			if(closed) throw new JouramException("Jouram is closed."); // no need to test for open, must be open to reach here
			
			// do the actual stuff BEFORE logging. If i do it after, if it throws exception the al would be unrecoverable
			// becouse recalling the method will launch the exception once again
	    	Object res;
			try {
				res = method.invoke(instance, args);
			} catch (InvocationTargetException e) {
				
				// get the naked exception out to the caller otherwise we change the logic of the interface.
				if(e.getTargetException() instanceof Exception) throw (Exception)e.getTargetException();
				else throw e; // if it's some other problem, throw it
			}
			
			// is a mutator method?
			String id = data.getIdByMethod(method);
	    	if(id != null)
	    	{
	    		worker.enqueue(new CmdMethodCall(new MethodCall(id, args)));
	    	}
		
	    	return res;
	    	
		}

	}

	public void commandSync()
	{
		synchronized(lock) 
		{
			CmdSync c = new CmdSync();
			worker.enqueue(c);
			try {
				c.done.get();
			} catch (Exception e) {
				throw new JouramException(e);
			}
		}
	}
	
	public void commandStartTransaction()
	{
		synchronized(lock) 
		{
			CmdStartTransaction c = new CmdStartTransaction();
			worker.enqueue(c);
		}
	}
	
	public void commandEndTransaction()
	{
		synchronized(lock) 
		{
			CmdEndTransaction c = new CmdEndTransaction();
			worker.enqueue(c);
		}
	}
	
	void doLogMethod(MethodCall mc) {
		checkWorkerThread();
		//  if so, journal this call
		LOG.debug(tag+"Journaling method {}", mc.methodId);
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeJournal(mc);
	}
	
	public void doLogStartTransaction() {
		checkWorkerThread();
		//  if so, journal this call
		LOG.info(tag+"Journaling start transaction");
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeStartTransaction();
	}
	
	public void doLogEndTransaction() {
		checkWorkerThread();
		//  if so, journal this call
		LOG.info(tag+"Journaling end transaction");
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeEndTransaction();
	}
	
		
	private void checkWorkerThread() {
		if(worker!=null) if(Thread.currentThread() != worker) throw new RuntimeException("Not on worker thread!");
	}

	public <E> E open(Class<E> yourInterface, E initialEmptyInstance) throws JouramException
	{
		try {
			return doOpen(yourInterface, initialEmptyInstance);
		
		} catch (Exception e) {
			LOG.error(tag+"Error opening database: "+e.getMessage(), e);
			throw new JouramException("Error opening database: "+e.getMessage(),e);
		}
	}

	
	
	@SuppressWarnings("unchecked")
	private <E> E doOpen(Class<E> yourInterface, E defaultInstance)	throws IOException, Exception {
		LOG.info(tag+"Opening Jouram");
		if(closed || (instance != null)) throw new JouramException("Jouram was already opened");
				
		currentDbVersion = manager.restorePreviousState();
		LOG.debug(tag+"Valid version identified: "+currentDbVersion);
		
		Path dbMainFile = manager.getPathForDbFile(currentDbVersion);
		Path dbJournal = manager.getPathForJournal(currentDbVersion);
		
		LOG.debug(tag+"Using version {} on files {}, {} ", currentDbVersion, dbMainFile, dbJournal);
		
		if(Files.exists(dbMainFile))
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
				Util.secureDelete(dbMainFile);
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
		
		worker = new JouramWorkerThread(this, new ArrayBlockingQueue<Cmd>(1000), this.manager.getDbName());
		// worker.setDaemon(true); better not, it may kill it too soon
		worker.start();
		
		return (E)myproxy;
	}

	private boolean handleJournal(Path dbJournal) throws Exception {
		
		if(Files.exists(dbJournal))
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
	private void replayJournal(Path dbJournal) throws Exception {
		long start = System.currentTimeMillis();
		int i = 0;
		int transactions = 0;
		try {
			List<MethodCall> transaction = new ArrayList<>();
			try(FileInputStream fis = new FileInputStream(dbJournal.toFile()))
			{
				Deserializer d = seder.deserializer(fis);
				
				while(true)
				{
					int cmd = d.getInputStream().read();
					
					switch (cmd) {
					
					case JournalImpl.WRITE_LOG:
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
							transaction.add(new MethodCall(methodId, parameters));
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
								m.invoke(instance, mc2.parameters);
								i++;
							}
							transaction.clear();
						}
						break;
						
					case -1:
						throw new EOFException("End of stream reached");
						
					default:
					//	throw new RuntimeException("Unexpected cmd");
					}
				}
			}
		
			
			
		} catch (EOFException e) {
			// ok, reached the end of file
		
		} catch (Exception e) {
			if(e.getMessage().contains("Buffer underflow")) // TODO: kryo specific
			{
				LOG.warn(tag+"Journal truncated, last call might not have been saved..");
			}
			else
			{
				throw e;
			}
		}

		LOG.info(tag+"Replayed "+i+" calls in "+(System.currentTimeMillis()-start)+" millis.");
		
	}
	

	public void commandSnapshot(long minimalJournalEntry) throws JouramException
	{
		synchronized(lock) 
		{
			// exit asap if journal is too short.
			// note: there may be calls in the queue to process but we're not counting them here
			// shouldn't make much difference
			if(journal.size() < minimalJournalEntry)
			{
				LOG.info(tag+"Not enought entries, no snapshot");
				return;
			}
			// if we need to snapshot, keep lock for the whole time to avoid concurrent modifications
			// this should ensure that:
			// 1) all concurrent thread access are kept outside (both for mutator and non-mutator)
			// 2) the queue is completely flushed and the snapshot done before continuing
			// TODO not sure it works. Deadlock ?
			CmdSnapshot c = new CmdSnapshot(minimalJournalEntry);
			worker.enqueue(c);
			try {
				c.done.get();
			} catch (Exception e) {
				throw new JouramException(e);
			}
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
			
			if(!Files.exists(manager.getPathForJournal(currentDbVersion)))
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

	public void commandClose() throws JouramException {
		synchronized(lock) {
			if (worker.closing)
			{
				LOG.info(tag+"Instance is already closing.");
				return;
			}
			CmdClose c = new CmdClose();
			worker.enqueue(c);
			try {
				c.done.get();
			} catch (Exception e) {
				throw new JouramException(e);
			}
		}
	}

	public void doClose() throws JouramException {
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
			
			doSnapshot(0); // this also closes the journal
			
			instance = null;
			data = null;
			
			LOG.info(tag+"Jouram closed, bye");
		}
	
	}
	
}
