package com.github.msx80.jouram.core.utils;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

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
			public InputStream getInputStream() {
				
				return ds;
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
				oo.flush();
			}

			@Override
			public void close() throws Exception {
				oo.close();
				
			}

			@Override
			public void flush() throws Exception {
				oo.flush();
				
			}

			@Override
			public OutputStream getOutputStream() {
				
				return oo;
			}
			
		
		};
	}

	@Override
	public Object register(Class<?> cls) {
		// TODO Auto-generated method stub
		return null;
	}

}
