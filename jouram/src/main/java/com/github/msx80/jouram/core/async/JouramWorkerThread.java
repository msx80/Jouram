package com.github.msx80.jouram.core.async;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.InstanceManager;
import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.queue.Cmd;
import com.github.msx80.jouram.core.queue.CmdClose;
import com.github.msx80.jouram.core.queue.CmdEndTransaction;
import com.github.msx80.jouram.core.queue.CmdMethodCall;
import com.github.msx80.jouram.core.queue.CmdSnapshot;
import com.github.msx80.jouram.core.queue.CmdStartTransaction;
import com.github.msx80.jouram.core.queue.CmdSync;
import com.github.msx80.jouram.core.queue.WaitingCmd;

public class JouramWorkerThread extends Thread {

	private static Logger LOG = LoggerFactory.getLogger(JouramWorkerThread.class);
	private InstanceManager instanceManager;
	
	/**
	 * This is the main queue used to communicate between InstanceManager and Jouram.
	 * Most of the Cmd messages are asyncronous, but Sync and Close will wait until completition
	 * eventually waiting for the queue to be spooled.
	 */
	private BlockingQueue<Cmd> queue;
	
	protected boolean closing = false;
	public Exception exception = null;
	private Consumer<Exception> exceptionListener;

	public JouramWorkerThread(InstanceManager instanceManager, BlockingQueue<Cmd> arrayBlockingQueue, Consumer<Exception> exceptionListener) {
		this.instanceManager = instanceManager;
		this.queue = arrayBlockingQueue;
		this.setName("Jouram Worker ["+instanceManager.manager.getDbName()+"]");
		this.exceptionListener = exceptionListener;
	}
	
	public void run()
	{
		try
		{
			/* handling exception:
			 * If any of the command in the queue throws an exception, we can't notify the actuall call site as it's already went forward due to asynchronicity
			 * Best thing to do is to report the exception as soon as possible (in the enqueue() method). 
			 * Exceptions always exits the loop and terminate the thread.
			 * Also, all waiting Cmd should be woke up with the exception to not make them wait forever.
			 */
			try
			{
				while(true)
				{
					Cmd c = queue.take();
	
					int size = queue.size();
					LOG.trace("Queue size is {}", size);
					
					// if there are no more messages in queue, we should flush the stream in order not
					// to let stuff sit in the buffer indefinitely, in low throughput situations.
					// if things get hot, messages will accumulate and we'll not need to flush manually
					boolean shouldFlush = size == 0;
	
					if (c instanceof CmdMethodCall)
					{
						instanceManager.doLogMethod(((CmdMethodCall) c).mc, shouldFlush);
					}
					else if (c instanceof CmdStartTransaction)
					{
						instanceManager.doLogStartTransaction();
					}
					else if (c instanceof CmdEndTransaction)
					{
						instanceManager.doLogEndTransaction(shouldFlush);
					}
					else if(c instanceof CmdSync)
					{
						try
						{
							instanceManager.doSync();
							((CmdSync) c).done.complete(null);
						}
						catch(Exception e)
						{
							((CmdSync) c).done.completeExceptionally(e);	
							throw e;
						}
					}
					else if(c instanceof CmdSnapshot)
					{
						try
						{
							instanceManager.doSnapshot(((CmdSnapshot) c).minimalJournalEntry);
							((CmdSnapshot) c).done.complete(null);
						}
						catch(Exception e)
						{
							((CmdSnapshot) c).done.completeExceptionally(e);	
							throw e;
						}
					}
					else if(c instanceof CmdClose)
					{
						// note: since CmdClose is fully sincronized, there's no need to flush the queue
						try {
							instanceManager.doClose(((CmdClose)c).quick); // this is called on the finally too, but it's ok, it ignores subsequent calls and i need it here to complete the future after
						}
						finally
						{
							// closing = true; // not needed, is already done upstream on "enqueue"
							((WaitingCmd) c).done.complete(null);
						}
						break;
					}
					else 
					{
						throw new RuntimeException("Unknown command??");
					}
				}
			} catch (Exception e) {
				// if any exception is thrown 
				exception = e;
				closing = true;
				
				// wake up all waiting commands
				while (!queue.isEmpty()) {
					Cmd c;
					try {
						c = queue.take();
						if(c instanceof WaitingCmd) ((WaitingCmd) c).done.completeExceptionally(new JouramException("Jouram previously encountered an exception", exception));
					} catch (Exception e1) {
						// just go
					}
				}
			}
		}
		finally
		{
			if(exception != null)
				exceptionListener.accept(exception);
		}
	}

	public synchronized void enqueue(Cmd cmd)
	{
		
		if(exception != null)
		{
			throw new JouramException("Jouram previously encountered an exception", exception);
		}
		
		if(closing)
		{
			throw new JouramException("Jouram is closing, no more data accepted.");
		}
		
		if(cmd instanceof CmdClose)
		{
			closing = true;
		}
		
		
		try {
			queue.put(cmd);
		} catch (InterruptedException e) {
			throw new JouramException("interrupted!", e);
		}
		
	}
	
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}
}
