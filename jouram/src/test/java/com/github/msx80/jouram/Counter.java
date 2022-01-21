package com.github.msx80.jouram;

import com.github.msx80.jouram.core.Mutator;

public interface Counter {

	@Mutator void increment(int i);
	
	int get();
	
	@Mutator void incrementAndExplode(int i);
	@Mutator void incrementAndReallyExplode(int i);	
	
	void explode();

}
