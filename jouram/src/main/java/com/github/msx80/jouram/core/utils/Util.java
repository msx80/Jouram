package com.github.msx80.jouram.core.utils;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.core.fs.VFile;

public class Util {

	
	public interface Acceptor<T> {
		void accept(T t) throws Exception;

	}

	public static void objectToFile(SerializationEngine seder, VFile path, Object object) throws Exception
	{
		try(Serializer s = seder.serializer(path.write()))
		{
			s.write(object);
		}

	}

	public static byte[] objectToBuffer(SerializationEngine seder, Object object) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try(Serializer s = seder.serializer(baos))
		{
			s.write(object);
		}
		return baos.toByteArray();
	}

	
	public static <T> T objectFromFile(SerializationEngine seder, VFile file, Class<T> cls) throws Exception
	{
		
		try(Deserializer s = seder.deserializer(file.read()))
		{
			return s.read(cls);
		}
		
	}
	
	public static <T> void objectsFromFile(SerializationEngine seder, VFile file, Class<T> cls, Acceptor<T> consumer) throws Exception
	{
		
		try(Deserializer s = seder.deserializer(file.read()))
		{
			while(true)
			{
				T o = s.read(cls);
				consumer.accept(o);
			}
		}
	}
	
	
	public static <T> Iterator<T> makeNonMutant(Iterator<T> iterator)
	{
		return new NonMutantIterator<>(iterator);
	}

	/*
	public static void secureDelete(Path file) throws IOException {
		LOG.info("Deleting {}",file);
		Files.deleteIfExists(file);
		if(Files.exists(file)) throw new JouramException("File exists after deletion");
	}
	private final static Logger LOG = LoggerFactory.getLogger(Util.class);
	 */
}
