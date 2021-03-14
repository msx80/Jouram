package com.github.msx80.jouram.core.utils;

import java.io.EOFException;
import java.io.InputStream;

public interface Deserializer extends AutoCloseable {
	/** 
	 * This should throw EOFException to signal end of file.
	 * If an object was partially wrote, it should discard the partial object and return EOFException
	 */
	public <T> T read(Class<T> cls) throws EOFException, Exception;
	public InputStream getInputStream();
}
