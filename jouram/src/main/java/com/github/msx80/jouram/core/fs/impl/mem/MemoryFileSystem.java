package com.github.msx80.jouram.core.fs.impl.mem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.msx80.jouram.core.fs.SimpleFileSystem;
import com.github.msx80.jouram.core.fs.VFile;

public class MemoryFileSystem implements SimpleFileSystem {

	private static final List<MemoryInputStream> empty = new ArrayList<>();
	
	Map<String, Object> files = new HashMap<>();
	Map<String, List<MemoryInputStream>> readers = new HashMap<>();
	
	@Override
	public synchronized VFile getFile(String name) {
		
		return new MemoryFile(name, this);
	}

	protected synchronized boolean exist(MemoryFile memoryFile) {
		
		return files.containsKey(memoryFile.name);
	}

	protected synchronized void delete(MemoryFile memoryFile) throws IOException {
		Object cont = files.get(memoryFile.name);
		if(cont instanceof MemoryOutputStream) ((MemoryOutputStream) cont).breakStream();
		for (MemoryInputStream memoryInputStream : getReaders(memoryFile)) {
			memoryInputStream.breakStream();
		}
		files.remove(memoryFile.name);
	}

	protected synchronized InputStream read(MemoryFile memoryFile) throws IOException {
		Object cont = files.get(memoryFile.name);
		if(cont == null) throw new FileNotFoundException(memoryFile.name);
		if(cont instanceof byte[]) 
		{
			MemoryInputStream m = new MemoryInputStream((byte[]) cont, x -> {
				removeReader(memoryFile, x);
			});
			addReader(memoryFile, m);
			return m;
		}
		throw new IOException("File is being written");
	}

	private List<MemoryInputStream> getReaders(MemoryFile f)
	{
		return getReaders(f.name);
	}
	private List<MemoryInputStream> getReaders(String name)
	{
		return readers.getOrDefault(name, empty);
	}
	
	private void addReader(MemoryFile f, MemoryInputStream m) {
		
		readers.computeIfAbsent(f.name, k -> new ArrayList<>()).add(m);
	}

	private synchronized void removeReader(MemoryFile f, MemoryInputStream m) {
		List<MemoryInputStream> l = readers.get(f.name);
		if (l==null) {
			return;
		}
		l.remove(m);
		if(l.isEmpty()) readers.remove(f.name);		
	}

	public synchronized OutputStream write(final MemoryFile memoryFile) throws IOException {
		Object cont = files.get(memoryFile.name);
		if(cont instanceof OutputStream) throw new IOException("File is already being written");
		if (!getReaders(memoryFile).isEmpty()) {
			throw new IOException("File is being read");	
		}
		MemoryOutputStream b = new MemoryOutputStream((x) -> {
			synchronized (MemoryFileSystem.this) {
				files.put(memoryFile.name, x.toByteArray());
			}
		});
		files.put(memoryFile.name, b);
		return b;
	}

	public synchronized void dump() {
		System.out.println("DUMP:");
		files.entrySet().forEach(e -> {
			System.out.print("\t"+e.getKey()+"\t");
			Object o = e.getValue();
			if(o instanceof byte[]) { 
				int readers = getReaders(e.getKey()).size();
				System.out.println(((byte[]) o).length+" bytes"+(readers == 0 ?"":" ("+readers+" reading)"));
			}
			else if(o instanceof OutputStream) System.out.println("being written...");
			else throw new RuntimeException("unknown class content "+o.getClass().getCanonicalName());
		});
		
	}
	public synchronized void breakAll() {
		List<MemoryOutputStream> f = files.entrySet().stream().filter(e -> e.getValue() instanceof MemoryOutputStream).map(e -> (MemoryOutputStream)e.getValue()).collect(Collectors.toList());
		for (MemoryOutputStream m : f) {
			try {
				m.breakStream();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		files.keySet().forEach(s -> {
			getReaders(s).forEach(r -> {
				try {
					r.breakStream();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
			
		});
	}
	
	public boolean isBeingRead(String filename)
	{
		return !getReaders(filename).isEmpty();
	}

	public boolean isBeingWritten(String filename)
	{
		return files.get(filename) instanceof OutputStream;
	}
	

	public boolean exists(String filename)
	{
		return files.containsKey(filename);
	}
	
	public Set<String> allFiles()
	{
		return new HashSet<>(files.keySet());
	}
	
	public void removeLastBytesFromFile(String filename, int howManyToRemove)
	{
		byte[] buffer = (byte[]) files.get(filename);
		byte[] b2 = Arrays.copyOf(buffer, buffer.length-howManyToRemove);
		files.put(filename, b2);
	}
	
}
