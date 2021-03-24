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
	
	private Object instance = null;
	
	private DbVersion currentDbVersion;
	
	private Journal journal = null;
	
	private final VersionManager manager;
	
	boolean closed = false;

	private ClassData data;
	private final SerializationEngine seder;

	
	private JouramWorkerThread worker = null;
	

	public InstanceManager(SerializationEngine seder, Path dbFolder, String dbName) {
		LOG.info("Creating Jouram '"+dbName+"' in "+dbFolder.toAbsolutePath());
		manager = new VersionManager(dbFolder, dbName);
		this.seder = seder;
		
		journal = new JournalImpl(seder, manager);
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

	private synchronized Object callDelegate(Method method, Object[] args) throws Exception {

		
		// need to sincronize the instance: all calls will be serialized one after the other with no overlapping.
		// this is necessary, even if the actual object is not synchronized, becouse the log file is one and each
		// call must be written when it's performed and finished.
		
		 
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

	public void enqueueSyncAndWait()
	{
		CmdSync c = new CmdSync();
		worker.enqueue(c);
		try {
			c.done.get();
		} catch (Exception e) {
			throw new JouramException(e);
		}
	}
	
	public void enqueueStartTransaction()
	{
		CmdStartTransaction c = new CmdStartTransaction();
		worker.enqueue(c);
	}
	
	public void enqueueEndTransaction()
	{
		CmdEndTransaction c = new CmdEndTransaction();
		worker.enqueue(c);
	}
	
	void doLogMethod(MethodCall mc) {
		checkWorkerThread();
		//  if so, journal this call
		LOG.debug("Journaling method {}", mc.methodId);
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeJournal(mc);
	}
	
	public void doLogStartTransaction() {
		checkWorkerThread();
		//  if so, journal this call
		LOG.info("Journaling start transaction");
		if(!journal.isOpen()) journal.open(currentDbVersion); 
		journal.writeStartTransaction();
	}
	
	public void doLogEndTransaction() {
		checkWorkerThread();
		//  if so, journal this call
		LOG.info("Journaling end transaction");
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
			LOG.error("Error opening database: "+e.getMessage(), e);
			throw new JouramException("Error opening database: "+e.getMessage(),e);
		}
	}

	
	
	@SuppressWarnings("unchecked")
	private <E> E doOpen(Class<E> yourInterface, E defaultInstance)	throws IOException, Exception {
		LOG.info("Opening Jouram");
		if(closed || (instance != null)) throw new JouramException("Jouram was already opened");
				
		currentDbVersion = manager.restorePreviousState();
		LOG.debug("Valid version identified: "+currentDbVersion);
		
		Path dbMainFile = manager.getPathForDbFile(currentDbVersion);
		Path dbJournal = manager.getPathForJournal(currentDbVersion);
		
		LOG.debug("Using version {} on files {}, {} ", currentDbVersion, dbMainFile, dbJournal);
		
		if(Files.exists(dbMainFile))
		{
			LOG.info("Loading instance...");
			instance = (E)Util.objectFromFile(seder, dbMainFile, defaultInstance.getClass());
			LOG.info("Instance loaded!");
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
		
		worker = new JouramWorkerThread(this, new ArrayBlockingQueue<com.github.msx80.jouram.core.queue.Cmd>(1000));
		// worker.setDaemon(true); better not, it may kill it too soon
		worker.start();
		
		return (E)myproxy;
	}

	private boolean handleJournal(Path dbJournal) throws Exception {
		
		if(Files.exists(dbJournal))
		{
			
			LOG.info("Replaying journal...");
			
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
						transactions++;
						break;
						
					case JournalImpl.WRITE_END_TRANSACTION:
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
				LOG.warn("Journal truncated, last call might not have been saved..");
			}
			else
			{
				throw e;
			}
		}

		LOG.info("Replayed "+i+" calls in "+(System.currentTimeMillis()-start)+" millis.");
		
	}
	

	public void enqueueSnapshot(int minimalJournalEntry) throws JouramException
	{
		worker.enqueue(new CmdSnapshot(minimalJournalEntry));
	}
	

	public void doSync() {
		checkWorkerThread();
		long start = System.currentTimeMillis();
		LOG.info("Syncing");
		
		journal.flush();
		LOG.info("Synced in "+(System.currentTimeMillis()-start)+" millis.");
	}

	
	public void doSnapshot(int minimalJournalEntry) throws JouramException
	{
		// doesn't need to be synchronized as it is called in the worker thread sequentially
		checkWorkerThread();
		long start = System.currentTimeMillis();
			LOG.info("Saving snapshot");
			
			if(!Files.exists(manager.getPathForJournal(currentDbVersion)))
			{
				// if we have no journal we don't need to save.
				// it would even be dangerous as we could end up with two db files and no journal. 
				LOG.info("No journal, no need to snapshot");
				return;
			}
			
			if(journal.size() < minimalJournalEntry)
			{
				LOG.info("Not enought entries, no snapshot");
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
				
				LOG.info("Snapshot saved succesfully in "+(System.currentTimeMillis()-start)+" millis.");
			}
			catch(JouramException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				LOG.error("Error closing Jouram: "+e.getMessage(), e);
				throw new JouramException("Error closing Jouram: "+e.getMessage(), e);
			}
	}

	private void saveInstance(DbVersion version) throws Exception {

		Util.objectToFile(seder, manager.getPathForDbFile(version), instance);
		
	}

	public synchronized void enqueueClose() throws JouramException {
		if (worker.closing)
		{
			LOG.info("Instance is already closing.");
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

	public void doClose() throws JouramException {
		// doesn't need to be synchronized as it is called in the worker thread sequentially
		checkWorkerThread();
		
		if(closed)
		{
			// Already closed, returning..
		}
		else
		{
			LOG.info("Closing Jouram");
			closed = true;
			
			doSnapshot(0); // this also closes the journal
			
			instance = null;
			data = null;
			
			LOG.info("Jouram closed, bye");
		}
	
	}
	
}
