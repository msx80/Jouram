package com.github.msx80.jouram.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * A SerializationEngine based on standard java serialization
 */
public class SerializableSeder implements SerializationEngine {

	@Override
	public Deserializer deserializer(InputStream is)  throws Exception {
		
		final ObjectInputStream ds = new ObjectInputStream(is);
		return new Deserializer() {
			
			@SuppressWarnings("unchecked")
			@Override
			public <T> T read(Class<T> cls)  throws Exception{
				return (T) ds.readObject();
			}

			@Override
			public void close() throws Exception {
				ds.close();
				
			}

			@Override
			public int readByte() throws IOException {
				return ds.read();
			}
		
		
		};
	}

	@Override
	public Serializer serializer(OutputStream os) throws Exception {
		final ObjectOutputStream oo = new ObjectOutputStream(os);
		return new Serializer() {
			
			@Override
			public void write(Object o) throws Exception {
				oo.writeObject(o);
			}

			@Override
			public void close() throws Exception {
				oo.close();
				
			}

			@Override
			public void flush() throws Exception {
				oo.flush();
			}

			
			public void writeByte(int o) throws Exception
			{
				oo.write(o);
			}
		
		};
	}

}
