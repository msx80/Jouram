package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.examples.simple.StringDb;
import com.github.msx80.jouram.examples.simple.StringDbImpl;

class BasicTest extends BaseTest {

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void fileCyclingTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		MemoryFileSystem mfs = new MemoryFileSystem();
		final StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async);
		try
		{
			assertEquals(mfs.allFiles(), set("mypath/demo.A.jdb"));
			db.add("test1");
			Jouram.sync(db);
			assertEquals(mfs.allFiles(), set("mypath/demo.A.jdb", "mypath/demo.A.jou"));
			db.add("test2");
			Jouram.snapshot(db, 0);
			assertEquals(mfs.allFiles(), set("mypath/demo.B.jdb"));
			db.add("last");
		}
		finally
		{
			Jouram.close(db);
			assertEquals(mfs.allFiles(), set("mypath/demo.C.jdb"));
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void contentTest(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		
		MemoryFileSystem mfs = new MemoryFileSystem();
		
		{
			final StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async);
			try
			{
				assertEquals(db.all(), list());
				db.add("ciao");
				db.add("second");
				assertEquals(db.all(), list("ciao", "second"));
			}
			finally
			{
				Jouram.close(db);
	
			}
		}
		
		{
			final StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async);
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

}
