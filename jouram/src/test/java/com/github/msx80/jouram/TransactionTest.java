package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.examples.simple.StringDb;
import com.github.msx80.jouram.examples.simple.StringDbImpl;

class TransactionTest extends BaseTest{

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void interruptedTransaction(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();

		final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),   cls.newInstance(), async);

		db.increment(1);
		db.increment(1);
		db.increment(1);
		Jouram.startTransaction(db);
		db.increment(1);
		db.increment(1);
		// Jouram.sync(db);
		Jouram.kill(db);
		final Counter db2 = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),   cls.newInstance(), async);
		try
		{
			int n = db2.get();
			assertEquals(n, 3);
		}
		finally
		{
			Jouram.close(db2);
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void completeTransaction(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();

		final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),   cls.newInstance(), async);

		db.increment(1);
		db.increment(1);
		db.increment(1);
		Jouram.startTransaction(db);
		db.increment(1);
		db.increment(1);
		Jouram.endTransaction(db);
		Jouram.kill(db);
		final Counter db2 = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),   cls.newInstance(), async);
		try
		{
			int n = db2.get();
			assertEquals(n, 5);
		}
		finally
		{
			Jouram.close(db2);
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void endWithoutStart(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			final StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(), cls.newInstance(), async);
			if(async) {
				Jouram.endTransaction(db);
				
				// wait for the worker to process the queue
				while(getWorkerThread("demo")!=null) {Thread.sleep(10);}
				
				// now any action will throw the previous exception
				Exception e = assertThrows(JouramException.class, () -> db.add("ciao"));
				assertTrue(e.getMessage().contains("Jouram previously encountered an exception"));
				assertTrue(e.getCause().getMessage().contains("Not in a transaction"));
			}
			else
			{
				Exception e = assertThrows(JouramException.class, () -> Jouram.endTransaction(db));
				assertTrue(e.getMessage().contains("Not in a transaction"));	
				
			}
			Jouram.kill(db);
		}
		
	}
	
}
