package com.github.msx80.jouram.examples.settings;

import java.util.Set;

import com.github.msx80.jouram.core.Mutator;

public interface Settings {

	public String get(String key);
	@Mutator
	public String set(String key, String value);
	public Set<String> keys();

}
