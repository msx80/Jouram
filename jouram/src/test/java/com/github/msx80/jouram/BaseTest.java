package com.github.msx80.jouram;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import com.github.msx80.jouram.core.async.JouramWorkerThread;
import com.github.msx80.jouram.core.utils.SerializableSeder;
import com.github.msx80.jouram.elsa.ElsaSeder;
import com.github.msx80.jouram.kryo.KryoSeder;
import com.github.msx80.jouram.text.TextSeder;

public class BaseTest 
{
	protected static Stream<Arguments> provideParameters() {
	    return Stream.of(
	            Arguments.of(KryoSeder.class),
	            Arguments.of(TextSeder.class),	            
	            Arguments.of(SerializableSeder.class),
	            Arguments.of(ElsaSeder.class)
	    );
	}
	protected static Stream<Arguments> provideParameters2() {
	    return Stream.of(
	            Arguments.of(TextSeder.class, true),
	            Arguments.of(TextSeder.class, false),
	            Arguments.of(KryoSeder.class, true),
	            Arguments.of(KryoSeder.class, false),
	            Arguments.of(SerializableSeder.class, true),
	            Arguments.of(SerializableSeder.class, false),
	            Arguments.of(ElsaSeder.class, true),
	            Arguments.of(ElsaSeder.class, false)
	    );
	}	

	public static JouramWorkerThread getWorkerThread(String db) {
		JouramWorkerThread jwt = (JouramWorkerThread) getThreadByName("Jouram Worker ["+db+"]");
		return jwt;
	}
	
	public static Thread getThreadByName(String threadName) {
	    for (Thread t : Thread.getAllStackTraces().keySet()) {
	        if (t.getName().equals(threadName)) return t;
	    }
	    return null;
	}
	public static Set<String> set(String... strings)
	{
		return new HashSet<>(Arrays.asList(strings));
	}

	public static List<String> list(String... strings)
	{
		return Arrays.asList(strings);
	}
	



}
