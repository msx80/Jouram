package com.github.msx80.jouram.core.utils;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerializationEngine 
{
	Deserializer deserializer(InputStream is) throws Exception;
	Serializer serializer(OutputStream os) throws Exception;
}
