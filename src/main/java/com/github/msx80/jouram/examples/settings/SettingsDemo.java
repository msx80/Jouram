package com.github.msx80.jouram.examples.settings;

import java.nio.file.Paths;

import com.github.msx80.jouram.core.Jouram;

public class SettingsDemo {

	public static void main(String[] args) {

		// a simple demo to store settings in the form of a Map
		
		Settings mySettings = Jouram.open(Paths.get("."), "settings", Settings.class, new SettingsImpl());

		System.out.println("Current settings:");
		for (String k : mySettings.keys()) {
			System.out.println(k+"\t"+mySettings.get(k));
		}
		
		mySettings.set("sample.user","admin");
		mySettings.set("sample.pass","mypass");
		mySettings.set("sample.time",""+System.currentTimeMillis());
		
		Jouram.close(mySettings);

	}

}
