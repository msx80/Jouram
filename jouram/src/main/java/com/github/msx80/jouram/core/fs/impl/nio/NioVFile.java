package com.github.msx80.jouram.core.fs.impl.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.github.msx80.jouram.core.JouramException;
import com.github.msx80.jouram.core.fs.VFile;
import com.github.msx80.jouram.core.utils.Util;

class NioVFile implements VFile {

	public final Path path;

	public NioVFile(Path path) {
		super();
		this.path = path;
	}

	@Override
	public VFile resolve(String p) {
		return new NioVFile( this.path.resolve(p));
	}

	@Override
	public String printableComplete() {
		
		return path.toAbsolutePath().toString();
	}
	
	@Override
	public boolean exists() {
		return Files.exists( path);
	}

	@Override
	public void delete() throws IOException {
		Files.deleteIfExists(path);
		if(Files.exists(path)) throw new IOException("File exists after deletion");
	}

	@Override
	public InputStream read() throws IOException {
		return Files.newInputStream(  path );
	}

	@Override
	public OutputStream write() throws IOException {
		return Files.newOutputStream( path );
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NioVFile other = (NioVFile) obj;
		return Objects.equals(path, other.path);
	}
	
	@Override
	public String toString()
	{
		return printableComplete();
	}

	@Override
	public VFile parent() {
		return new NioVFile(path.getParent());
	}
	
}
