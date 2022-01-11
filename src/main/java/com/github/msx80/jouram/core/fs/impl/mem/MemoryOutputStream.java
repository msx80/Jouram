package com.github.msx80.jouram.core.fs.impl.mem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

class MemoryOutputStream extends OutputStream
{

	Consumer<MemoryOutputStream> onClose;
	ByteArrayOutputStream src;
	private boolean broken = false;
	
	public void breakStream() throws IOException
	{
		this.broken  = true;
		close();
	}
	
	public MemoryOutputStream(Consumer<MemoryOutputStream> onClose) {
		super();
		this.onClose = onClose;
		src = new ByteArrayOutputStream();
	}



	@Override
	public synchronized void close() throws IOException {
		src.close();
		if(onClose!=null)
		{
			Consumer<MemoryOutputStream> onClose2 = onClose;
			onClose=null;
			onClose2.accept(this);
		}
	}



	@Override
	public void write(int b) throws IOException {
		checkBroken();
		src.write(b);
		
	}



	private void checkBroken() throws IOException {
		if (broken) throw new IOException("File is broken (deleted or something)");
	}

	@Override
	public void write(byte[] b) throws IOException {
		checkBroken();
		src.write(b);
	}



	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		checkBroken();
		src.write(b, off, len);
	}



	public byte[] toByteArray() {
		
		return src.toByteArray();
	}

	
	
}
