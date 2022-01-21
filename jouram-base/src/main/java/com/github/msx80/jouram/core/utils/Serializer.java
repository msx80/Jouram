package com.github.msx80.jouram.core.utils;

public interface Serializer extends AutoCloseable
{
	
	public void write(Object o) throws Exception;
	public void close() throws Exception;
	public void flush() throws Exception;
	
	/**
	 * Write a byte in the underlying stream.
	 * Doesn't need to actually be a single byte in the final data, implementations are free to wrap 
	 * the byte in whatever container, as long as the matching "Deserializer.readByte" returns the same byte. 
	 * @param o
	 * @throws Exception
	 */
	public void writeByte(int o) throws Exception;
}
