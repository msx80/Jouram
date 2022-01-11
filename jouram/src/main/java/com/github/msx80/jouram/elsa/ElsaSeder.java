package com.github.msx80.jouram.elsa;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializer;

import com.github.msx80.jouram.core.utils.Deserializer;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Serializer;

public class ElsaSeder implements SerializationEngine {

	final ElsaSerializer elsa;

	public ElsaSeder() {
		this.elsa = new ElsaMaker().make();
	}
	
	public ElsaSeder(ElsaSerializer elsa) {
		this.elsa = elsa;
	}

	@Override
	public Deserializer deserializer(InputStream is) {
		final DataInputStream in = new DataInputStream(is);
		return new Deserializer() {
			
			@Override
			public <T> T read(Class<T> cls)  throws EOFException, Exception{
				
				
				return elsa.deserialize(in);
				
			}
			
			@Override
			public void close() throws IOException {
			
					in.close();
				
				
			}
		

			@Override
			public int readByte() throws Exception {
				return in.read();
			}
		};
	}

	@Override
	public Serializer serializer(OutputStream os) {
		final ElsaSerializer elsa = new ElsaMaker().make();
		final DataOutputStream out2 = new DataOutputStream(os);
		return new Serializer() {
			
			@Override
			public void write(Object o) throws IOException {
				
					elsa.serialize(out2, o);
				
				
			}
			
			@Override
			public void flush() throws IOException {
			
					out2.flush();
				
			}
			
			@Override
			public void close() throws IOException {
		
					out2.close();
			
				
			}

			@Override
			public void writeByte(int o) throws Exception {
				out2.write(o);
				
			}
		};
	}
}
