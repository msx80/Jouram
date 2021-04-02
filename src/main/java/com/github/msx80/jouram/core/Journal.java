package com.github.msx80.jouram.core;

public interface Journal {

	boolean isOpen();
	void open(DbVersion version);
	void writeJournal(MethodCall mc);
	void writeStartTransaction();
	void writeEndTransaction();
	void close();
	long size();
	void flush();
	
}
