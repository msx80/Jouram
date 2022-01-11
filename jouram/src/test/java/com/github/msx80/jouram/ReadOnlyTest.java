package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;

class ReadOnlyTest extends BaseTest{

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void readOnlyInstance(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();

		final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),   cls.newInstance(), async);
		try
		{
			Counter readOnly = Jouram.readOnly(db);
			db.increment(10);
			assertEquals(10, readOnly.get());
			db.increment(2);
			assertEquals(12, readOnly.get());
			
			assertThrows(UnsupportedOperationException.class, () -> readOnly.increment(10));
			
			assertThrows(RuntimeException.class, () -> readOnly.explode());
			
		}
		finally
		{
			Jouram.close(db);
		}
	}
	
}
