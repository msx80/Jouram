package com.github.msx80.jouram.core.fs.impl.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.msx80.jouram.core.fs.VFile;

public class LogFile implements VFile {

	VFile src;

	public LogFile(VFile src)
	{
		this.src = src;
	}
	
	public VFile resolve(String p) {
		log("Resolving "+p+" from "+this);
		return new LogFile(src.resolve(p));
	}

	public String printableComplete() {
		return src.printableComplete();
	}

	public boolean exists() {
		boolean ex = src.exists();
		log("Exists "+this+" -> "+ex);
		return ex;
	}

	public void delete() throws IOException {
		log("Deleting "+this);
		src.delete();
	}

	public InputStream read() throws IOException {
		log("Reading from "+this);
		return new LogInputStream(src.read(), x -> log("Finished reading from "+this));
	}

	public OutputStream write() throws IOException {
		log("Writing to "+this);
		return new LogOutputStream(src.write(), x -> log("Finished writing to "+this));
	}

	private void log(String msg) {
		System.out.println(msg);
		
	}
	
	public String toString()
	{
		return src.printableComplete();
	}

	@Override
	public VFile parent() {
		return new LogFile(src.parent());
	}
	


}
