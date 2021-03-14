package com.github.msx80.jouram.examples.stress;

import java.io.Serializable;
import java.util.Date;

public class Msg implements Serializable {

	private static final long serialVersionUID = 8424139714757913547L;
	
	public final Date instant;
	public final String text;
	
	/*
	public Msg()
	{
		instant = null;
		text = null;
	}
	*/
	
	public Msg(Date instant, String text) {
		super();
		this.instant = instant;
		this.text = text;
	}
	
	
}
