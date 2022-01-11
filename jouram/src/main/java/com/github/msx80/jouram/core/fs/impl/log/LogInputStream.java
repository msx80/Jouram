package com.github.msx80.jouram.core.fs.impl.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@Deprecated
public class LogInputStream extends InputStream {

	InputStream src;
	Consumer<LogInputStream> onClose;

	
	
	public LogInputStream(InputStream src, Consumer<LogInputStream> onClose) {
		super();
		this.src = src;
		this.onClose = onClose;
	}

	public int read() throws IOException {
		return src.read();
	}

	public int hashCode() {
		return src.hashCode();
	}

	public int read(byte[] b) throws IOException {
		return src.read(b);
	}

	public boolean equals(Object obj) {
		return src.equals(obj);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return src.read(b, off, len);
	}

	public long skip(long n) throws IOException {
		return src.skip(n);
	}

	public String toString() {
		return src.toString();
	}

	public int available() throws IOException {
		return src.available();
	}

	public void close() throws IOException {
		try
		{
			if(onClose!=null)
			{
				Consumer<LogInputStream> onClose2 = onClose;
				onClose=null;
				onClose2.accept(this);
			}
		}
		finally
		{
			src.close();
		}
	}

	public void mark(int readlimit) {
		src.mark(readlimit);
	}

	public void reset() throws IOException {
		src.reset();
	}

	public boolean markSupported() {
		return src.markSupported();
	}
	
}
