package com.github.msx80.jouram.elsa;

public class ElsaSeder  /*implements Seder*/ {
/*
	@Override
	public Deserializer deserializer(InputStream is) {
		final ElsaSerializer elsa = new ElsaMaker().make();
		final DataInputStream in = new DataInputStream(is);
		return new Deserializer() {
			
			@Override
			public <T> T read(Class<T> cls) {
				
				try {
					return elsa.deserialize(in);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			@Override
			public void close() {
				try {
					in.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}
			
			@Override
			public boolean available() {
				
				try {
					return in.available() > 0;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@Override
	public Serializer serializer(OutputStream os) {
		final ElsaSerializer elsa = new ElsaMaker().make();
		final DataOutputStream out2 = new DataOutputStream(os);
		return new Serializer() {
			
			@Override
			public void write(Object o) {
				try {
					elsa.serialize(out2, o);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}
			
			@Override
			public void flush() {
				try {
					out2.flush();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}
			
			@Override
			public void close() {
				try {
					out2.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}
		};
	}
*/
}
