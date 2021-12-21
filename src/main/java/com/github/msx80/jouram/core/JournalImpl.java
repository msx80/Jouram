package com.github.msx80.jouram.core;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Serializer;

public class JournalImpl implements Journal{

	public static final byte WRITE_LOG = 1;
	public static final byte WRITE_START_TRANSACTION = 2;
	public static final byte WRITE_END_TRANSACTION = 3;

	private Serializer journalStream = null;
	private final VersionManager manager;
	private final SerializationEngine seder;

	private long numJournalEntries = 0;
	private boolean autoSynch;
	private int transactionDepth = 0;
	private static final Object[] empty = new Object[0];
	
	public JournalImpl(SerializationEngine seder, VersionManager manager, boolean autoSync) {
		this.seder = seder;
		this.manager = manager;
		this.autoSynch = autoSync;
	}

	private void openJournal(DbVersion version2) 
	{
		Path dbJournal = manager.getPathForJournal(version2);
		// open journal
		if (Files.exists(dbJournal)) {
			throw new JouramException("Journal file already exists "+dbJournal);
		}
		
		try {
			journalStream = seder.serializer(new FileOutputStream(dbJournal.toFile()));
		} catch (Exception e) {
			throw new JouramException("Error opening journal stream", e);
		}
		
	}

	
	
	
	@Override
	public void writeJournal(MethodCall mc) {
		if(journalStream == null) throw new JouramException("Journal is not open");
		try
		{
			numJournalEntries ++;
			journalStream.getOutputStream().write(WRITE_LOG);
			journalStream.write(mc.methodId);
			journalStream.write(mc.parameters == null ? empty :mc.parameters);

			// if we are in transaction, no point in synchin every call even if we are in autosync.
			if(autoSynch && (transactionDepth == 0) ) journalStream.flush(); 
		}
		catch(Exception e)
		{
			LOG.error("Could not journalize method call to "+mc.methodId+" exception "+e.getMessage()+". Closing Jouram to avoid corruption.", e);
			close();
			throw new JouramException("Could not write journal for method call "+mc.methodId+", Jouram closed to prevent journal corruption.", e);
		}
	}

	@Override
	public void close() {
		Serializer old = journalStream;
		journalStream = null;
		numJournalEntries = 0;
		try {
			if(old!=null) old.close();
		} catch (Exception e) {
			LOG.error("Exception while closing journal: "+e.getMessage(), e);
			e.printStackTrace();
		}

	}

	private final static Logger LOG = LoggerFactory.getLogger(JournalImpl.class);

	@Override
	public boolean isOpen() {
		return journalStream != null;
	}

	@Override
	public void open(DbVersion version) {
		try {
			openJournal(version);
		} catch (Exception e) {
			throw new JouramException("Exception opening journal",e);
		}
		
	}

	@Override
	public long size() {
		return numJournalEntries;
	}

	@Override
	public void flush() {
		if (journalStream!=null) {
			try {
				journalStream.flush();
			} catch (Exception e) {
				throw new JouramException(e);
			}
		}
	}

	@Override
	public void writeStartTransaction() {
		if(journalStream == null) throw new JouramException("Journal is not open");
		
		try
		{
			transactionDepth++;
			numJournalEntries ++;
			journalStream.getOutputStream().write(WRITE_START_TRANSACTION);
			// journalStream.flush(); doesn't make sense to flush here
		}
		catch(Exception e)
		{
			LOG.error( "Could not journalize start transaction exception "+e.getMessage()+". Closing Jouram to avoid corruption.", e);
			close();
			throw new JouramException("Could not write journal for start transaction, Jouram closed to prevent journal corruption.", e);
		}
	}

	@Override
	public void writeEndTransaction() {
		if(journalStream == null) throw new JouramException("Journal is not open");
		try
		{
			transactionDepth--;
			numJournalEntries ++;
			journalStream.getOutputStream().write(WRITE_END_TRANSACTION);
			if(autoSynch) journalStream.flush(); 
		}
		catch(Exception e)
		{
			LOG.error("Could not journalize end transaction exception "+e.getMessage()+". Closing Jouram to avoid corruption.", e);
			close();
			throw new JouramException("Could not write journal for end transaction, Jouram closed to prevent journal corruption.", e);
		}
	}

	
}
