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

class KillTest extends BaseTest {

	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void contentTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			final StringDb db = Jouram.open(mfs.getFile("mypath"), "ct", StringDb.class, new StringDbImpl(), cls.newInstance(), async);
			assertEquals(db.all(), list());
			db.add("ciao");
			db.add("second");
			Jouram.sync(db);
			assertEquals(db.all(), list("ciao", "second"));
			
			//killJouramAndCloseFiles(mfs, "demo", async);
			Jouram.kill(db);
			
			mfs.dump();
		}
		
		{
			final StringDb db = Jouram.open(mfs.getFile("mypath"), "ct", StringDb.class, new StringDbImpl(),  cls.newInstance(), async);
			try
			{
				assertEquals(db.all(), list("ciao", "second"));
			}
			finally
			{
				Jouram.close(db);
			}
		
		}
			
		
	}

	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void killAndContinue(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		{
			final StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(), cls.newInstance(), async);
			assertEquals(db.all(), list());
			db.add("ciao");
			db.add("second");
			Jouram.sync(db);
			assertEquals(db.all(), list("ciao", "second"));
			
			mfs.breakAll();
			
			if(async) {
				db.add("boom"); // this goes well becouse it's asyncronous
				
				// wait for the worker to process the queue
				while(getWorkerThread("demo")!=null) {Thread.sleep(10);}
				
				// now any action will throw the previous exception
				Exception e = assertThrows(JouramException.class, () -> db.add("ciao"));
				assertTrue(e.getMessage().contains("Jouram previously encountered an exception"));
				assertTrue(e.getCause().getMessage().contains("Could not write journal for method call"));
				assertTrue(e.getCause().getCause().getMessage().contains("File is broken"));
			}
			else
			{
				Exception e = assertThrows(JouramException.class, () -> db.add("ciao"));
				assertTrue(e.getMessage().contains("Could not write journal for method call"));
				assertTrue(e.getCause().getMessage().contains("File is broken"));	
				
			}
			Jouram.kill(db);
		}
		
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void useAfterKill(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		
		final StringDb db = Jouram.open(mfs.getFile("mypath"), "ct", StringDb.class, new StringDbImpl(), cls.newInstance(), async);
		db.add("ciao");
		db.add("second");
		Jouram.kill(db);
		
		Exception e = assertThrows(JouramException.class, () -> {db.add("after");});
		assertTrue(e.getMessage().contains("Jouram is closed"));
			
		
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void useAfterClose(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		
		final StringDb db = Jouram.open(mfs.getFile("mypath"), "ct", StringDb.class, new StringDbImpl(), cls.newInstance(), async);
		db.add("ciao");
		db.add("second");
		Jouram.close(db);
		
		Exception e = assertThrows(JouramException.class, () -> {db.add("after");});
		assertTrue(e.getMessage().contains("Jouram is closed"));
			
		
	}

	

}
