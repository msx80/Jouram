package com.github.msx80.jouram.core;

import java.io.Serializable;

/**
 * A class that store info about a specific method call, to be stored in the journal.
 *
 */
public class MethodCall implements Serializable{

	private static final long serialVersionUID = -1916380312977126493L;

	public final String methodId;
	public final Object[] parameters;

	public MethodCall(String methodId, Object[] parameters) {
		super();
		this.methodId = methodId;
		this.parameters = parameters;
	}
	
	
	
}
