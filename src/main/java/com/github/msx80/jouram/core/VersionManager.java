package com.github.msx80.jouram.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.fs.VFile;

public class VersionManager {

	
	private VFile dbFolder;
	private String dbName;
	// private FileSystem fs;
	
	Map<DbVersion, VFile> dbFiles = null;
	Map<DbVersion, VFile> joFiles = null;
	
	public VersionManager(VFile dbFolder, String dbName) {
		super();
		
		this.dbFolder = dbFolder;
		this.dbName = dbName;
		
		dbFiles = new HashMap<>();
		joFiles = new HashMap<>();
		 
		for (DbVersion v : DbVersion.values())
		{
			dbFiles.put(v, getPathForDbFile(v));
			joFiles.put(v, getPathForJournal(v));
		}
		LOG.debug( dbFiles+" : "+joFiles);
	}

	public DbVersion restorePreviousState() throws IOException {
		
		LOG.info("Restoring previous state");
		
		DbVersion journalVersion = null; // this is the current journal version
		
		for (DbVersion v : DbVersion.values()) {
			if(joFiles.get(v).exists())
			{
				if(journalVersion != null) throw new JouramException("More than one journal file found."); 
				journalVersion = v;
			}	
		}
		
		LOG.info("journal version found: {}",journalVersion);
		
		if(journalVersion == null)
		{
			// no journal file found. 
			// in this case there's zero or one DB file, which is good to use.
			return caseNoJournal();
		}
		else
		{
			// we have exacly one journal file. Let's see what db files we have.
			return caseOneJournal(journalVersion);
		}
	}

	public VFile getPathForJournal(DbVersion v) {
		return dbFolder.resolve(dbName+"."+v+".jou");
	}

	public VFile getPathForDbFile(DbVersion v) {
		return dbFolder.resolve(dbName+"."+v+".jdb");
	}
	
	public void deleteDb(DbVersion v) throws IOException
	{
		getPathForDbFile(v).delete();
	}
	public void deleteJournal(DbVersion v) throws IOException
	{
		getPathForJournal(v).delete();;
	}

	private DbVersion caseOneJournal(DbVersion journalVersion) throws IOException 
	{
		VFile prvvvDb = dbFiles.get(journalVersion.prev());
		VFile currDb = dbFiles.get(journalVersion);
		VFile nextDb = dbFiles.get(journalVersion.next());
		
		if(prvvvDb.exists())
		{
			throw new JouramException("A db with previous version than the current journal exists.");
		}
		else if(currDb.exists() && nextDb.exists())
		{
			// must have been killed while the new db file was being written
			// since we don't know if it was written correctly, we revert to the previous one.
			LOG.info("Found current and next db, deleting next");
			nextDb.delete();
			return journalVersion;
		}
		else if (currDb.exists())
		{
			// only the current db exists. This is the usual case if app is killed before committing.
			// just use the current files.
			LOG.info("Found a single db, using: {}",journalVersion);
			return journalVersion;
		}
		else if (nextDb.exists())
		{
			// the next file already exists, but not the current. Rare case in which a commit already removed the current db but not the current journal.
			LOG.info("Found next db, removing stale Journal {}",journalVersion);
			deleteJournal(journalVersion);
			return journalVersion.next();
		}
		else
		{
			// just the journal exists, not the db file ??
			throw new JouramException("Found a journal without the db file.");
		}
	}

	private DbVersion caseNoJournal() {
		
		// we are in the situation of having no journal at all. There should be one or zero db, if it's there it should be ok.
		DbVersion dbVersion = null; 
		for (DbVersion v : DbVersion.values()) {
			if(dbFiles.get(v).exists())
			{
				if(dbVersion != null) throw new JouramException("Two db files found with no journal."); 
				dbVersion = v;
			}	
		}
		LOG.info("Db version found {}",dbVersion);
		// return the DB found or at worst start with A as there's not db or journal file whatsoever.
		return dbVersion == null ? DbVersion.A : dbVersion;
		
	}
	
	private final static Logger LOG = LoggerFactory.getLogger(VersionManager.class);

	public String getDbName() {
		return dbName;
	}
	
}
