package com.github.msx80.jouram.core.utils;

import java.io.OutputStream;

public interface Serializer extends AutoCloseable{
	public void write(Object o) throws Exception;
	public void close() throws Exception;
	public void flush() throws Exception;
	public OutputStream getOutputStream();
}
