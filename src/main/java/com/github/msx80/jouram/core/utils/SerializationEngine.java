package com.github.msx80.jouram.core.utils;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerializationEngine {
	/**
	 * For serializers that require class registration beforehand
	 * @param cls
	 * @return
	 */
	Object register(Class<?> cls);
	Deserializer deserializer(InputStream is) throws Exception;
	Serializer serializer(OutputStream os) throws Exception;
}
