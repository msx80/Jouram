package com.github.msx80.jouram.text;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.msx80.jouram.core.utils.Deserializer;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Serializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class TextSeder implements SerializationEngine {

	protected static final String TERMINATOR = "*END*";
	protected static final String ARRAY = "*ARRAY*";
	final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public TextSeder() {
	}

	@Override
	public Deserializer deserializer(InputStream is) {
		final BufferedReader w = new BufferedReader( new InputStreamReader(is) );
		return new Deserializer() {
			
			@Override
			public <T> T read(Class<T> cls)  throws EOFException, Exception{
				
				String className = lineOrEof();
				if(className.equals(ARRAY))
				{
					int q = Integer.parseInt(lineOrEof());
					Object[] res = new Object[q];
					for (int i = 0; i < res.length; i++) {
						res[i] = read(null);
					}
					return (T) res;
				}
				else
				{
					List<String> lines = new ArrayList<>();
					while(true)
					{
						String s = lineOrEof();
						if(s.equals(TERMINATOR)) break;
						lines.add(s);
					}
					return (T) gson.fromJson(lines.stream().collect(Collectors.joining("\n")), Class.forName(className));
				}
				
				
			}

			private String lineOrEof() throws IOException, EOFException {
				String className = w.readLine();
				if(className == null) throw new EOFException("reached end of line!");
				return className;
			}
			
			@Override
			public void close() throws IOException {
			
					w.close();
				
				
			}
		

			@Override
			public int readByte() throws Exception {
				String l = w.readLine();
				if(l==null) return -1;
				return Integer.parseInt(l);
			}
		};
	}

	@Override
	public Serializer serializer(OutputStream os) {
		Writer w = new OutputStreamWriter(os);
		return new Serializer() {
			boolean isEmpty=true;
			private void writeLine(String s) throws IOException
			{
				if(isEmpty)
				{
					isEmpty=false;
					w.write(s);
				}
				else
					w.write("\n"+s);
			}
			@Override
			public void write(Object o) throws IOException {
				if(o instanceof Object[])
				{
					writeLine(ARRAY);
					Object[] a = ((Object[])o);
					writeLine( ""+a.length);
					for (Object oo : a) {
						write(oo);
					}
				}
				else
				{
					String className = o.getClass().getName();
					writeLine(className);
					writeLine(gson.toJson(o));
					writeLine(TERMINATOR);
				}
			}
			
			@Override
			public void flush() throws IOException {
			
					w.flush();
				
			}
			
			@Override
			public void close() throws IOException {
		
					w.close();
			
				
			}

			@Override
			public void writeByte(int o) throws Exception {
				writeLine(""+o);
				
			}
		};
	}
}
