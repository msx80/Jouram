package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;

class TruncatedJournalTest extends BaseTest{

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void truncatedJournal(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();
		
		{
			final Counter db = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),  cls.newInstance(), async);
	
			db.increment(1);
			db.increment(1);
			db.increment(1);
			Jouram.kill(db);
			
			mfs.removeLastBytesFromFile("mypath/counter.A.jou", 1);
		}
		
		final Counter db2 = Jouram.open(mfs.getFile("mypath"), "counter", Counter.class, new CounterImpl(),   cls.newInstance(), async);
		try
		{
			int n = db2.get();
			assertEquals(n, 2);
		}
		finally
		{
			Jouram.close(db2);
		}
	}
	
}
