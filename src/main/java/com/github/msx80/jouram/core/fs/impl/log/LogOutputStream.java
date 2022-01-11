package com.github.msx80.jouram.core.fs.impl.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

@Deprecated
public class LogOutputStream extends OutputStream {

	OutputStream src;
	Consumer<LogOutputStream> onClose;

	public LogOutputStream(OutputStream src, Consumer<LogOutputStream> onClose) {
		super();
		this.src = src;
		this.onClose = onClose;
	}

	public void write(int b) throws IOException {
		src.write(b);
	}

	public int hashCode() {
		return src.hashCode();
	}

	public void write(byte[] b) throws IOException {
		src.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		src.write(b, off, len);
	}

	public boolean equals(Object obj) {
		return src.equals(obj);
	}

	public void flush() throws IOException {
		src.flush();
	}

	public void close() throws IOException {
		try
		{
			if(onClose!=null)
			{
				Consumer<LogOutputStream> onClose2 = onClose;
				onClose=null;
				onClose2.accept(this);
			}
		}
		finally
		{
			src.close();
		}
	}

	public String toString() {
		return src.toString();
	}
	
	
}
