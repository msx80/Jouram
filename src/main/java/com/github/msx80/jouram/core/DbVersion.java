package com.github.msx80.jouram.core;

/**
 * This class represent a version of the database currently used.
 * There are three versions, that are used ciclically. If we are currently writing
 * the B version, on next snapshot we will move to the C version, and so on.
 * This system ensure there's always a "before" and "after" version than the current one
 * 
 */
enum DbVersion {
	A, B, C;
	
	public DbVersion next() 
	{
		return values()[(ordinal()+1) % values().length];
	}
	
	public DbVersion prev() 
	{
		int s = values().length;
		return values()[(ordinal()+s-1) % s];
	}
}
