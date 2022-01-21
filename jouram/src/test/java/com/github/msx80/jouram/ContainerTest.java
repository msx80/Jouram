package com.github.msx80.jouram;

import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.map.JouramMap;
import com.github.msx80.jouram.core.utils.SerializationEngine;

class ContainerTest extends BaseTest {

	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void contentTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			Map<String, BigDecimal> map = Jouram.map(String.class, BigDecimal.class).serializationEngine(cls.newInstance()).folder(mfs.getFile("map")).dbName("map").async(async).open();
			map.put("ciao", BigDecimal.valueOf(10));
			
			Jouram.snapshot(map, 0);
			map.computeIfAbsent("prova", k -> BigDecimal.valueOf(5));
			map.computeIfAbsent("xx", k -> BigDecimal.valueOf(53));
			map.remove("xx", BigDecimal.valueOf(53));
			Jouram.kill(map);
		}
		{
			Map<String, BigDecimal> map = Jouram.map(String.class, BigDecimal.class).serializationEngine(cls.newInstance()).folder(mfs.getFile("map")).dbName("map").async(async).open();
			try
			{
				assertTrue(map.containsKey("ciao"));
				assertTrue(map.containsKey("prova"));
				assertTrue(!map.containsKey("xx"));
			}
			finally
			{
				Jouram.kill(map);
			}
		}
	}

}
