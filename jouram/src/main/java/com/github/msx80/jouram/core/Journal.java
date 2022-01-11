package com.github.msx80.jouram.core;

public interface Journal {

	boolean isOpen();
	void open(DbVersion version);
	void writeJournal(MethodCall mc, boolean shouldFlush);
	void writeStartTransaction();
	void writeEndTransaction(boolean shouldFlush);
	void close();
	long size();
	void flush();
	
}
