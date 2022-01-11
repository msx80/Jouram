package com.github.msx80.jouram.core.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.EngineBridge;
import com.github.msx80.jouram.core.InstanceManager;
import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.MethodCall;
import com.github.msx80.jouram.core.queue.Cmd;
import com.github.msx80.jouram.core.queue.CmdClose;
import com.github.msx80.jouram.core.queue.CmdEndTransaction;
import com.github.msx80.jouram.core.queue.CmdMethodCall;
import com.github.msx80.jouram.core.queue.CmdSnapshot;
import com.github.msx80.jouram.core.queue.CmdStartTransaction;
import com.github.msx80.jouram.core.queue.CmdSync;
import com.github.msx80.jouram.core.queue.WaitingCmd;

public class AsyncBridge implements EngineBridge {
	private static Logger LOG = LoggerFactory.getLogger(AsyncBridge.class);
	
	
	private JouramWorkerThread worker = null;
	
	private String tag;
	
	public AsyncBridge(String tag, InstanceManager manager, Consumer<Exception> exceptionListener) {
		super();
		this.tag = tag;
		worker = new JouramWorkerThread(manager, new ArrayBlockingQueue<Cmd>(1000), exceptionListener);
		// worker.setDaemon(true); better not, it may kill it too soon
		worker.start();
		
	}

	@Override
	public void commandSync()
	{
		
			CmdSync c = new CmdSync();
			worker.enqueue(c);
			try {
				c.done.get();
			} catch (JouramException e) {
				throw e;
			} catch (ExecutionException e) {
				if(e.getCause() instanceof JouramException) throw (JouramException)(e.getCause());
				throw new JouramException(e);
			} catch (Exception e) {
				throw new JouramException(e);
			}
		
	}
	@Override
	public void commandCallMethod(String id, Object[] args, boolean withException) {
		worker.enqueue(new CmdMethodCall(new MethodCall(id, args, withException)));
	}

	@Override
	public void commandStartTransaction()
	{
		
			CmdStartTransaction c = new CmdStartTransaction();
			worker.enqueue(c);
	
	}
	
	@Override
	public void commandEndTransaction()
	{
		
			CmdEndTransaction c = new CmdEndTransaction();
			worker.enqueue(c);
		
	}
	
	@Override
	public void commandSnapshot(long minimalJournalEntry) throws JouramException
	{
		
			// if we need to snapshot, keep lock for the whole time to avoid concurrent modifications
			// this should ensure that:
			// 1) all concurrent thread access are kept outside (both for mutator and non-mutator)
			// 2) the queue is completely flushed and the snapshot done before continuing
			CmdSnapshot c = new CmdSnapshot(minimalJournalEntry);
			worker.enqueue(c);
			try {
				c.done.get();
			} catch (Exception e) {
				throw new JouramException(e);
			}
		
	}
	
	@Override
	public void commandClose(boolean quick) throws JouramException {
		
		if (worker.closing)
		{
			LOG.info(tag+"Instance is already closing.");
			return;
		}
		WaitingCmd c = new CmdClose(quick);
		worker.enqueue(c);
		try {
			c.done.get();
		} catch (Exception e) {
			throw new JouramException(e);
		}
		
		try {
			worker.join();
		} catch (InterruptedException e) {
			throw new JouramException(e);
		}
	}
	
	@Override
	public void checkWorkerThread() {
		if(worker!=null) if(Thread.currentThread() != worker) throw new RuntimeException("Not on worker thread!");
	}
	
}
