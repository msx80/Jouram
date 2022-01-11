package com.github.msx80.jouram.core;

public interface EngineBridge {

	void commandSync();

	void commandCallMethod(String id, Object[] args, boolean withException);

	void commandStartTransaction();

	void commandEndTransaction();

	void commandSnapshot(long minimalJournalEntry) throws JouramException;

	void commandClose(boolean quick) throws JouramException;

	void checkWorkerThread();

}