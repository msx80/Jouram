package com.github.msx80.jouram.core.utils;

public interface Serializer extends AutoCloseable
{
	public void write(Object o) throws Exception;
	public void close() throws Exception;
	public void flush() throws Exception;
	public void writeByte(int o) throws Exception;
}
