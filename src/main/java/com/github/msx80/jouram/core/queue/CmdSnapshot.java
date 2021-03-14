package com.github.msx80.jouram.core.queue;

public class CmdSnapshot implements Cmd {

	public final int minimalJournalEntry;

	public CmdSnapshot(int minimalJournalEntry) {
		super();
		this.minimalJournalEntry = minimalJournalEntry;
	}
	
	
}
