package com.github.msx80.jouram.core.fs;

import java.io.IOException;
import java.io.OutputStream;

public interface SimpleFileSystem 
{
	VFile getFile(String name);
	
	default void write(String name, byte[] data) throws IOException
	{
		try (OutputStream os = getFile(name).write())
		{
			os.write(data);
		}
	}
}
