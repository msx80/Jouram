package com.github.msx80.jouram.core.queue;

public final class CmdClose extends WaitingCmd {
	
	public final boolean quick;

	public CmdClose(boolean quick) {
		super();
		this.quick = quick;
	}
	
	
}
