package com.github.msx80.jouram.core;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	boolean closing = false;
	Exception exception = null;

	public JouramWorkerThread(InstanceManager instanceManager, BlockingQueue<Cmd> arrayBlockingQueue) {
		this.instanceManager = instanceManager;
		this.queue = arrayBlockingQueue;
		this.setName("Jouram Worker");
	}
	
	public void run()
	{
		/* handling exception:
		 * If any of the command in the queue throws an exception, we can't notify the actuall call site as it's already went forward due to asynchronicity
		 * Best thing to do is to report the exception as soon as possible (in the enqueue() method). 
		 * Also, all waiting Cmd should be woke up with the exception to not make them wait forever.
		 */
		try
		{
			while(true)
			{
				LOG.trace("Queue size is {}", queue.size());
				Cmd c = queue.take();
				if (c instanceof CmdMethodCall)
				{
					instanceManager.doLogMethod(((CmdMethodCall) c).mc);
				}
				else if (c instanceof CmdStartTransaction)
				{
					instanceManager.doLogStartTransaction();
				}
				else if (c instanceof CmdEndTransaction)
				{
					instanceManager.doLogEndTransaction();
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
					try {
						instanceManager.doClose(); // this is called on the finally too, but it's ok, it ignores subsequent calls and i need it here to complete the future after
					}
					finally
					{
						((CmdClose) c).done.complete(null);
					}
					break;
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
				} catch (InterruptedException e1) {
					// just go
				}
			}
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
}
