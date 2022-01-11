package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;

class MultithreadingTest extends BaseTest {

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void multuthreadingTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();
		final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
		try
		{
			Runnable r = () -> doSomeInc(db);
			Thread[] t = new Thread[100];
			for (int i = 0; i < t.length; i++) {
				t[i] = new Thread(r);
				t[i].start();
			}
			for (int i = 0; i < t.length; i++) {
				t[i].join();
			}
		}
		finally
		{
			Jouram.close(db);
		}
		final Counter db2 = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
		try {
			assertEquals(100000, db2.get());
		} finally {
			Jouram.close(db2);
		}
	}
	
	private void doSomeInc(Counter db) {
		for (int i = 0; i < 500; i++) {
			db.increment(1);
		}
		Jouram.snapshot(db, 100);
		for (int i = 0; i < 500; i++) {
			db.increment(1);
		}
	}

	
}
