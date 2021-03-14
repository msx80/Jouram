package com.github.msx80.jouram.examples.simple;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


/**
 * The implementation of StringDb, backed by a list.
 */
public class StringDbImpl implements StringDb, Serializable
{
	private static final long serialVersionUID = 2689371136601449977L;

	private List<String> data = new LinkedList<>();
	
	@Override
	public void add(String s)
	{
		data.add(s);
	}
	
	@Override
	public void remove(String s)
	{
		data.remove(s);
	}
	
	public int size()
	{
		return data.size();
	}
	

	@Override
	public void print() {
		for (String s : data) {
			System.out.println("- "+s);
		}
		
	}


}

