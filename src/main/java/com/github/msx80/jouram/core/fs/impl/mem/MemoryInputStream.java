package com.github.msx80.jouram.core.fs.impl.mem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

class MemoryInputStream extends InputStream {

	ByteArrayInputStream bis;
	Consumer<MemoryInputStream> onClose;
	
	boolean broken = false;
	
	public MemoryInputStream(byte[] buf,Consumer<MemoryInputStream> onClose) {
		super();
		this.onClose = onClose;
		this.bis = new ByteArrayInputStream(buf);
	}
	
	public void breakStream() throws IOException
	{
		this.broken  = true;
		close();
	}

	private void checkBroken() throws IOException {
		if (broken) throw new IOException("File is broken (deleted or something)");
	}


	@Override
	public int read() throws IOException {
		checkBroken();
		return bis.read();
	}



	@Override
	public int read(byte[] b) throws IOException {
		checkBroken();
		return bis.read(b);
	}



	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		checkBroken();
		return bis.read(b, off, len);
	}



	@Override
	public int available() throws IOException {
		checkBroken();
		return bis.available();
	}



	@Override
	public synchronized void close() throws IOException {
		bis.close();
		if(onClose!=null)
		{
			Consumer<MemoryInputStream> onClose2 = onClose;
			onClose=null;
			onClose2.accept(this);
		}
	}

	
	
}
