package com.github.msx80.jouram.core.queue;

import java.util.concurrent.CompletableFuture;

public abstract class WaitingCmd implements Cmd {
	public final CompletableFuture<Void> done;
	
	public WaitingCmd()
	{
		done = new CompletableFuture<>();
	}

}
