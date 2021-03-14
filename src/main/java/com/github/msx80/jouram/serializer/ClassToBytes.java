package com.github.msx80.jouram.serializer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ClassToBytes {
	

	
	public byte[] objectToByte(Object o)
	{
		return null;
	}
	
	public static void main(String[] args) throws IllegalAccessException
	{
		ClassProfile c = ClassProfile.calculate(Test.class);
		System.out.println(c);
		
		Test a = new Test();
		System.out.println(a);
		FieldUtils.writeField(a, "pc", "sovrascritto", true);
		
		System.out.println(a);
		
		
		//a.c="CIAO A TUTTI COME STATE?";
		byte[] b1;
		{	
			final Kryo k = new Kryo();
			//k.register(jouram.serializer.Test.class);
			k.setRegistrationRequired(false);
			k.writeObject(new Output(new ByteArrayOutputStream()),new ArrayList<String>(10));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Output o = new Output(baos);
			k.writeObject(o, a);
			o.flush();
			o.close();
			b1 = baos.toByteArray();		
		}
		
		byte[] b2;
		{
			final Kryo k = new Kryo();
			//k.register(jouram.serializer.Test.class);
			k.setRegistrationRequired(false);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Output o = new Output(baos);
			k.writeObject(o, a);
			o.flush();
			o.close();
			b2 = baos.toByteArray();		
		}
		
		System.out.println(Arrays.toString(b1));
		System.out.println(Arrays.toString(b2));
		
		{
			final Kryo k = new Kryo();
			//k.register(jouram.serializer.Test.class);
			k.setRegistrationRequired(false);
			Input i = new Input(b1);
			Test b = k.readObject(i, Test.class);
			System.out.println(b);
		}
		
		//final Input ais = new Input (is);
	}
	
}
