package com.github.msx80.jouram.core.fs.impl.log;

import com.github.msx80.jouram.core.fs.SimpleFileSystem;
import com.github.msx80.jouram.core.fs.VFile;

public class LogFileSystem implements SimpleFileSystem {

	SimpleFileSystem src;
	
	public LogFileSystem(SimpleFileSystem src) {
		this.src = src;
	}

	@Override
	public VFile getFile(String name) {
		
		return new LogFile( src.getFile(name) );
	}

}
