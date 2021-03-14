package com.github.msx80.jouram.examples.simple;

import com.github.msx80.jouram.core.Mutator;

/**
 * A very simple database of strings.
 * Support adding and removing of strings, as well as printing them and checking the size.
 * 
 */
public interface StringDb {

	/**
	 * Add the specified string to the database
	 * @param s
	 */
	@Mutator
	public abstract void add(String s);

	/**
	 * Remove the specified string from the database
	 * @param s
	 */
	@Mutator
	public abstract void remove(String s);

	/**
	 * Return the number of strings currently stored in the database
	 * @return
	 */
	public abstract int size();
	
	/**
	 * Prints the entire content of the database.
	 */
	public abstract void print();
	
}