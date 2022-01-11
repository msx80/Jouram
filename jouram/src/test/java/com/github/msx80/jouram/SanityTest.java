package com.github.msx80.jouram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.fs.impl.mem.MemoryFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;
import com.github.msx80.jouram.core.utils.Serializer;
import com.github.msx80.jouram.examples.simple.StringDb;
import com.github.msx80.jouram.examples.simple.StringDbImpl;

class SanityTest extends BaseTest {

	
	@ParameterizedTest
	@MethodSource("provideParameters2")
	void twoDb(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		mfs.write("mypath/demo.A.jdb", "ciao".getBytes());
		mfs.write("mypath/demo.B.jdb", "ciao".getBytes());
		
		Exception e = assertThrows(JouramException.class, () -> Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async));
		assertTrue(e.getMessage().contains("Two db files found with no journal."));
	}

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void twoDbAndAJournalKO(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		mfs.write("mypath/demo.A.jdb", "ciao".getBytes());
		mfs.write("mypath/demo.B.jdb", "ciao".getBytes());
		mfs.write("mypath/demo.B.jou", "ciao".getBytes());
		
		Exception e = assertThrows(JouramException.class, () -> Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async));
		assertTrue(e.getMessage().contains("A db with previous version than the current journal exists."));
	}

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void twoDbAndAJournal2KO(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		/*
		mfs.write("mypath/demo.C.jdb", "ciao".getBytes());
		mfs.write("mypath/demo.B.jdb", "ciao".getBytes());
		mfs.write("mypath/demo.B.jou", "ciao".getBytes());
		*/
		SerializationEngine ks = cls.newInstance();
		Serializer s = ks.serializer(mfs.getFile("mypath/demo.B.jdb").write());
		
		StringDbImpl x = new StringDbImpl();
		x.add("ciao");
		s.write( x);
		s.close();
		
		mfs.write("mypath/demo.C.jdb", new byte[0]);
		mfs.write("mypath/demo.B.jou", new byte[0]);
		
		StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async);
		try
		{
			assertEquals(db.all(), Arrays.asList("ciao"));
		}
		finally
		{
			Jouram.close(db);
		}
		mfs.dump();
	}

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void singleJournal(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		mfs.write("mypath/demo.B.jou", "ciao".getBytes());
		
		Exception e = assertThrows(JouramException.class, () -> Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async));
		assertTrue(e.getMessage().contains("Found a journal without the db file."));
	}

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void twoJournals(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		mfs.write("mypath/demo.B.jou", "ciao".getBytes());
		mfs.write("mypath/demo.A.jou", "ciao".getBytes());
		
		Exception e = assertThrows(JouramException.class, () -> Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(), cls.newInstance(), async));
		assertTrue(e.getMessage().contains("More than one journal file found."));
	}

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void twoDbAndAJournalOK(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		SerializationEngine ks = cls.newInstance();
		Serializer s = ks.serializer(mfs.getFile("mypath/demo.A.jdb").write());
		s.write( new StringDbImpl());
		s.close();
		
		mfs.write("mypath/demo.B.jdb", new byte[0]);
		mfs.write("mypath/demo.A.jou", new byte[0]);
		
		final StringDb db = Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async);
		Jouram.close(db);
	}

	@ParameterizedTest
	@MethodSource("provideParameters2")
	void dbAndDifferentJournal(Class<? extends SerializationEngine> cls, boolean async) throws Exception {
		
		MemoryFileSystem mfs = new MemoryFileSystem();

		SerializationEngine ks = cls.newInstance();
		Serializer s = ks.serializer(mfs.getFile("mypath/demo.A.jdb").write());
		s.write( new StringDbImpl());
		s.close();
		
		mfs.write("mypath/demo.B.jou", new byte[0]);
		
		Exception e = assertThrows(JouramException.class, () -> Jouram.open(mfs.getFile("mypath"), "demo", StringDb.class, new StringDbImpl(),  cls.newInstance(), async));
		assertTrue(e.getMessage().contains("A db with previous version than the current journal exists."));
	}


}
