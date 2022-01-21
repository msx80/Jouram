package com.github.msx80.jouram.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import com.github.msx80.jouram.Jouram;
import com.github.msx80.jouram.core.fs.VFile;
import com.github.msx80.jouram.core.fs.impl.nio.NioFileSystem;
import com.github.msx80.jouram.core.utils.SerializationEngine;

/**
 * Helper class for fluent api Jouram configuration
 *
 */
public final class JouramBuilder<E, R> {
	public VFile dbFolder;
	public String dbName;
	public Class<E> yourInterface;
	public E initialEmptyInstance;
	public SerializationEngine nullForDefault;
	public boolean async;
	private Function<JouramBuilder<E, R>, R> creator;
	
	public JouramBuilder(Class<E> yourInterface, E initialEmptyInstance, Function<JouramBuilder<E, R>, R> creator) {
		super();
		this.yourInterface = yourInterface;
		this.initialEmptyInstance = initialEmptyInstance;
		dbFolder = new NioFileSystem().getFile( Paths.get(".") );
		dbName = yourInterface.getSimpleName();
		async = false;
		this.creator = creator;
	}

	public JouramBuilder<E,R> folder(Path dbFolder) {
		this.dbFolder = new NioFileSystem().getFile( Paths.get(".") );
		return this;
	}

	public JouramBuilder<E,R> folder(VFile dbFolder) {
		this.dbFolder = dbFolder;
		return this;
	}

	public JouramBuilder<E,R> dbName(String dbName) {
		this.dbName = dbName;
		return this;
	}

	public JouramBuilder<E,R> serializationEngine(SerializationEngine nullForDefault) {
		this.nullForDefault = nullForDefault;
		return this;
	}

	public JouramBuilder<E,R> async(boolean async) {
		this.async = async;
		return this;
	}
	
	public R open()
	{
		return creator.apply(this);
		
	}
	
	
}
