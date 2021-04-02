package com.github.msx80.jouram.core.queue;

public class CmdSnapshot 

	extends WaitingCmd // TODO added as a temporary solution to a concurrency problem:
						// when enqueuing a snapshot and not syncing, some other mutator method may be called
						// before the snapshot happens (or during the snapshot), causing either the snapshot 
						// of a modified instance (different from the time when snapshot was called) or a 
						// concurrent modification exception. As a quick fix, i'll make Snapshot a waiting command
						// so it won't return until it's done.
	implements Cmd {

	public final long minimalJournalEntry;

	public CmdSnapshot(long minimalJournalEntry) {
		super();
		this.minimalJournalEntry = minimalJournalEntry;
	}
	
	
}
