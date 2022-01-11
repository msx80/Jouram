package com.github.msx80.jouram.core.utils;

import java.io.EOFException;

public interface Deserializer extends AutoCloseable {
	/** 
	 * This should throw EOFException to signal end of file.
	 * If an object was partially read, it should discard the partial data and return EOFException
	 */
	public <T> T read(Class<T> cls) throws EOFException, Exception;
	
	/**
	 * Read a byte from the stream, or return -1 if the stream has terminated (same as InputStream.read() )
	 * @return
	 * @throws Exception
	 */
	public int readByte() throws Exception;
}
