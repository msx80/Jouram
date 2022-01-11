package com.github.msx80.jouram.core;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.msx80.jouram.Jouram;
import com.github.msx80.jouram.core.fs.VFile;
import com.github.msx80.jouram.core.fs.impl.nio.NioFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;

/**
 * Helper class for fluent api Jouram configuration
 *
 * @param <E>
 */
public class JouramSetup<E> {
	VFile dbFolder;
	String dbName;
	Class<E> yourInterface;
	E initialEmptyInstance;
	SerializationEngine nullForDefault;
	boolean async;
	
	public JouramSetup(Class<E> yourInterface, E initialEmptyInstance) {
		super();
		this.yourInterface = yourInterface;
		this.initialEmptyInstance = initialEmptyInstance;
		dbFolder = new NioFileSystem().getFile( Paths.get(".") );
		dbName = yourInterface.getSimpleName();
		async = false;
	}

	public JouramSetup<E> folder(Path dbFolder) {
		this.dbFolder = new NioFileSystem().getFile( Paths.get(".") );
		return this;
	}

	public JouramSetup<E> folder(VFile dbFolder) {
		this.dbFolder = dbFolder;
		return this;
	}

	public JouramSetup<E> dbName(String dbName) {
		this.dbName = dbName;
		return this;
	}

	public JouramSetup<E> serializationEngine(SerializationEngine nullForDefault) {
		this.nullForDefault = nullForDefault;
		return this;
	}

	public JouramSetup<E> async(boolean async) {
		this.async = async;
		return this;
	}
	
	public E open()
	{
		return Jouram.open(dbFolder, dbName, yourInterface, initialEmptyInstance, nullForDefault, async);
	}
	
	
}
