package com.github.msx80.jouram.core.fs.impl.nio;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import com.github.msx80.jouram.core.fs.SimpleFileSystem;
import com.github.msx80.jouram.core.fs.VFile;

public class NioFileSystem implements SimpleFileSystem {

	private java.nio.file.FileSystem srcFs;

	public NioFileSystem() {
		this.srcFs = FileSystems.getDefault();
	}
	
	public NioFileSystem(java.nio.file.FileSystem srcFs) {
		this.srcFs = srcFs;
	}
	
	public VFile getFile(Path path)
	{
		return new NioVFile(path);
	}
	
	@Override
	public VFile getFile(String name) {
		
		return new NioVFile( srcFs.getPath(name) );
	}

}
