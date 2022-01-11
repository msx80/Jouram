package com.github.msx80.jouram.kryo;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import com.github.msx80.jouram.core.utils.Deserializer;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Serializer;

public class KryoSeder implements SerializationEngine {

	final Kryo k;

	public KryoSeder() {
		k = new Kryo();
		k.setRegistrationRequired(false);
		k.setDefaultSerializer(VersionFieldSerializer.class);
	}

	public KryoSeder(Kryo kryo) {
		k = kryo;
	}
	
	@Override
	public Deserializer deserializer(InputStream is) {
		
		final Input ais = new Input (is);
				
		return new Deserializer() {
			
			
			@Override
			public <T> T read(Class<T> cls) throws Exception{
				try
				{
					return k.readObject(ais, cls);
				}
				catch (KryoException e) {
					// kryo doesn't report clearly then it encounters an EOF.
					// look inside the message to detect it.
					// see https://github.com/EsotericSoftware/kryo/issues/872
					if(e.getMessage().contains("Buffer underflow"))
					{
						throw new EOFException("Buffer underflow");
					}
					throw e;
				}
			}
			
			@Override
			public void close() {
				ais.close();
				
			}
			@Override
			public int readByte() {
				return ais.read();
			}
		
		};
	}

	@Override
	public Serializer serializer(OutputStream os) {
		
		final Output aos = new Output(os);
		return new Serializer() {
			
			@Override
			public void write(Object o) {
				
				k.writeObject(aos, o);
				
			}
			
			@Override
			public void close() {
				aos.close();
				
			}

			@Override
			public void flush() {
				aos.flush();
				
			}
			
			public void writeByte(int o) throws Exception
			{
				aos.write(o);
			}
		};
	}

}
