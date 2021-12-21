package com.github.msx80.jouram.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.utils.Util;

class VersionManager {

	
	private Path dbFolder;
	private String dbName;
	
	Map<DbVersion, Path> dbFiles = null;
	Map<DbVersion, Path> joFiles = null;
	
	public VersionManager(Path dbFolder, String dbName) {
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
			if(Files.exists(joFiles.get(v)))
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

	public Path getPathForJournal(DbVersion v) {
		return dbFolder.resolve(dbName+"."+v+".jou");
	}

	public Path getPathForDbFile(DbVersion v) {
		return dbFolder.resolve(dbName+"."+v+".jdb");
	}
	
	public void deleteDb(DbVersion v) throws IOException
	{
		Util.secureDelete(getPathForDbFile(v));
	}
	public void deleteJournal(DbVersion v) throws IOException
	{
		Util.secureDelete(getPathForJournal(v));
	}

	private DbVersion caseOneJournal(DbVersion journalVersion) throws IOException 
	{
		Path prvvvDb = dbFiles.get(journalVersion.prev());
		Path currDb = dbFiles.get(journalVersion);
		Path nextDb = dbFiles.get(journalVersion.next());
		
		if(Files.exists(prvvvDb))
		{
			throw new JouramException("A db with previous version than the current journal exists.");
		}
		else if(Files.exists(currDb) && Files.exists(nextDb))
		{
			// must have been killed while the new db file was being written
			// since we don't know if it was written correctly, we revert to the previous one.
			LOG.info("Found current and next db, deleting next");
			Util.secureDelete(nextDb);
			return journalVersion;
		}
		else if (Files.exists(currDb))
		{
			// only the current db exists. This is the usual case if app is killed before committing.
			// just use the current files.
			LOG.info("Found a single db, using: {}",journalVersion);
			return journalVersion;
		}
		else if (Files.exists(nextDb))
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
			if(Files.exists(dbFiles.get(v)))
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
