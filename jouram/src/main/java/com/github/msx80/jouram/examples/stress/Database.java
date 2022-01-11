package com.github.msx80.jouram.examples.stress;

import java.util.Date;

import com.github.msx80.jouram.core.Mutator;

public interface Database {

	@Mutator
	void addMessage(Date instant, String message);
	
	int count();
	
	void dump();
}
