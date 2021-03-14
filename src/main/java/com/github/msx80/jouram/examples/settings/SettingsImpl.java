package com.github.msx80.jouram.examples.settings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class SettingsImpl extends HashMap<String, String> implements Settings {

	private static final long serialVersionUID = 3359310264186368471L;

	@Override
	public Set<String> keys() {
		
		return Collections.unmodifiableSet(super.keySet());
	}

	@Override
	public String get(String key) {
		
		return super.get(key);
	}

	@Override
	public String set(String key, String value) {
		return super.put(key, value);
	}

	

	

}
