package com.github.msx80.jouram.serializer;

import java.lang.reflect.Field;

public class ClassField implements Comparable<ClassField> {
	public final int version;
	public final Field field;
	
	public ClassField(int version, Field field) {
		super();
		this.version = version;
		this.field = field;
	}

	@Override
	public int compareTo(ClassField o) {
		int res = Integer.compare(this.version, o.version);
		if(res == 0) res =this.field.getName().compareTo(o.field.getName());
		return res;
	}
	
}
