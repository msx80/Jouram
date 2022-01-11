package com.github.msx80.jouram.core.fs.impl.mem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import com.github.msx80.jouram.core.fs.VFile;

class MemoryFile implements VFile {

	public static final String SEP = "/";
	
	protected String name;
	private MemoryFileSystem fs;

	public MemoryFile(String name, MemoryFileSystem memoryFileSystem) {
		this.name = name;
		this.fs = memoryFileSystem;
	}

	@Override
	public VFile resolve(String p) {
		String n = name;
		if(!name.endsWith(SEP)) n += SEP;
		n+=p;
		return new MemoryFile(n, fs);
	}

	@Override
	public String printableComplete() {
		return name;
	}

	@Override
	public boolean exists() {
		
		return fs.exist(this);
	}

	@Override
	public void delete() throws IOException {
		fs.delete(this);

	}

	@Override
	public InputStream read() throws IOException {
		
		return fs.read(this);
	}

	@Override
	public OutputStream write() throws IOException {
		return fs.write(this);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MemoryFile other = (MemoryFile) obj;
		return Objects.equals(fs, other.fs) && Objects.equals(name, other.name);
	}

	@Override
	public String toString()
	{
		return printableComplete();
	}

	@Override
	public VFile parent() {
		String n = name;
		if(name.endsWith(SEP)) n = name.substring(0,name.length()-SEP.length());
		int f = n.lastIndexOf(SEP);
		String nn = n.substring(0, f);
		
		return new MemoryFile(nn, fs);
	}

	
}
