package com.github.msx80.jouram.serializer;

public class Test {
	public static String a = "ciao";
	public static final String b = "ciaooo";
	public String c = "c";
	public final String d = "d";
	@SederVersion(5)
	private final String add2 = "d";
	private static String pa = "ciao";
	private static final String pb = "ciaooo";
	private String pc = "c";
	private final String pd = "d";
	@SederVersion(2)
	private String addendum = "c";
	public AAAAAB aaaa = new AAAAAB();
	
	
	@Override
	public String toString() {
		return "Test [c=" + c + ", d=" + d + ", pc=" + pc + ", pd=" + pd + "]"+aaaa;
	}
	
	
}
