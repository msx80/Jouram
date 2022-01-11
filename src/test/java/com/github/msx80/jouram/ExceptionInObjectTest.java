package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;

class ExceptionInObjectTest extends BaseTest {

	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void exceptionTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
			db.increment(1);
			try {
				db.incrementAndExplode(1);
				fail("Should have thrown Exception");
			} catch (Exception e) {
				// exploded
			}
			Jouram.kill(db);
			// killJouramAndCloseFiles(mfs, "counter", async);
		}
		
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
			try
			{
				assertEquals(db.get(), 2);
			}
			finally
			{
				Jouram.close(db);
			}
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void errorTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
			db.increment(1);
			try {
				db.incrementAndReallyExplode(1);
				fail("Should have thrown Exception");
			} catch (Throwable e) {
				// exploded
			}
			Jouram.kill(db);
		}
		
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
			try
			{
				assertEquals(db.get(), 1);
			}
			finally
			{
				Jouram.close(db);
			}
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void exceptionTransactionTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
			Jouram.transactional(db, () ->{
				db.increment(1);
				db.increment(1);
				try {
					db.incrementAndExplode(1);
					fail("Should have thrown Exception");
				} catch (Exception e) {
					// exploded
				}
			});
			Jouram.kill(db);
			// killJouramAndCloseFiles(mfs, "counter", async);
		}
		
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
			try
			{
				assertEquals(3, db.get());
			}
			finally
			{
				Jouram.close(db);
			}
		}
	}
	
}
