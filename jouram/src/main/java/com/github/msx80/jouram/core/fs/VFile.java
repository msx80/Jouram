package com.github.msx80.jouram.core.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.msx80.jouram.core.fs.impl.nio.NioFileSystem;

public interface VFile 
{
	
	VFile resolve(String p);
	VFile parent();

	String printableComplete();

	boolean exists();

	void delete() throws IOException;
	
	InputStream read() throws IOException;
	
	OutputStream write() throws IOException;
	
	default Reader readChars(Charset cs) throws IOException
	{
		return new InputStreamReader(read(), cs);
	}	
	default Writer writeChars(Charset cs) throws IOException
	{
		return new OutputStreamWriter(write(), cs);
	}
	
	default List<String> readAllLines(Charset cs) throws IOException
	{
		try (BufferedReader reader = new BufferedReader(readChars(cs))) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                result.add(line);
            }
            return result;
        }
	}
	
	public static VFile fromPath(Path p)
	{
		return new NioFileSystem().getFile( p );
	}
}
