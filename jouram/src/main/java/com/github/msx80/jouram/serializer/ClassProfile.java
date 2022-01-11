package com.github.msx80.jouram.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassProfile {
	public final ClassField[] fields;
	public final Class<?> clss;
	
	
	
	public ClassProfile(Class<?> clss, ClassField[] fields) {
		super();
		this.clss = clss;
		this.fields = fields;
	}



	public static final ClassProfile calculate(Class<?> c)
	{
		Field[] ff = null;// FieldUtils.getAllFields(c);
		
		List<ClassField> res = new ArrayList<>();
		
		for (Field f : ff) {
			int m = f.getModifiers();
			boolean skip = Modifier.isStatic(m) || Modifier.isTransient(m);
			if(!skip)
			{
				int version = f.isAnnotationPresent(SederVersion.class) ? f.getAnnotation(SederVersion.class).value() : 1;
				ClassField cf = new ClassField(version, f);
				res.add(cf);
			}
		}
		
		ClassField[] arr = res.toArray(new ClassField[res.size()]);
		
		Arrays.sort(arr);
		
		return new ClassProfile(c, arr);
	}



	@Override
	public String toString() {
		String s = "Profile: "+clss.getCanonicalName();
		for (ClassField f : fields) {
			s+="\n"+f.field.getName()+" "+f.version;
		}
		
		return s;
	}
	
	
	
}
