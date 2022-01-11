package com.github.msx80.jouram;

import java.io.Serializable;

public class CounterImpl implements Counter, Serializable {

	private static final long serialVersionUID = -7952357551663639465L;

	private int n = 0;
	
	@Override
	public void increment(int i) {
		n+=i;
	}

	@Override
	public int get() {
		return n;
	}

	@Override
	public void incrementAndExplode(int i) {
		n+=i;
		throw new RuntimeException("BOOM!");
		
	}

	@Override
	public void incrementAndReallyExplode(int i) {
		n+=i;
		throw new OutOfMemoryError("Not really, just testing");
	}
	
	@Override
	public void explode()
	{
		throw new RuntimeException("BOOM!");
	}

}
