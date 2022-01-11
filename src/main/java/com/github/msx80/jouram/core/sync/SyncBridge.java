package com.github.msx80.jouram.core.sync;

import java.util.function.Consumer;

import com.github.msx80.jouram.core.EngineBridge;
import com.github.msx80.jouram.core.InstanceManager;
import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.MethodCall;

public class SyncBridge implements EngineBridge {

	private InstanceManager instanceManager;
	private Consumer<Exception> exceptionListener;

	public SyncBridge(InstanceManager instanceManager2, Consumer<Exception> exceptionListener) {
		this.instanceManager = instanceManager2;
		this.exceptionListener = exceptionListener;
	}

	@Override
	public void commandSync() {
		capture( () -> 	instanceManager.doSync() );
	}

	@Override
	public void commandCallMethod(String id, Object[] args, boolean withException) {
		capture( () -> 	instanceManager.doLogMethod(new MethodCall(id, args, withException), true) );

	}

	@Override
	public void commandStartTransaction() {
		capture( () -> 	instanceManager.doLogStartTransaction() );
	}

	@Override
	public void commandEndTransaction() {
		capture( () -> 	instanceManager.doLogEndTransaction(true) );
	}

	@Override
	public void commandSnapshot(long minimalJournalEntry) throws JouramException {
		capture( () -> 	instanceManager.doSnapshot(minimalJournalEntry) );

	}

	@Override
	public void commandClose(boolean quick) throws JouramException {
		capture( () -> 	instanceManager.doClose(quick) );
	}
	
	private void capture(Runnable r)
	{
		try {
			r.run();
		} catch (Exception e) {
			exceptionListener.accept(e);
			throw e;
		}
	}

	@Override
	public void checkWorkerThread() {
		

	}

}
