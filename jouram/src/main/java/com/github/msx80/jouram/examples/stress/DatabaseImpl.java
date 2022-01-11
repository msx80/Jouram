package com.github.msx80.jouram.examples.stress;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseImpl implements Database, Serializable {

	private static final long serialVersionUID = -3191560621909016389L;
	
	List<Msg> msgs = new ArrayList<>();
	
	@Override
	public void addMessage(Date instant, String message) {
		msgs.add(new Msg(instant, message));
	}

	@Override
	public int count() {
		return msgs.size();
	}

	@Override
	public void dump() {
		for (Msg msg : msgs) {
			System.out.println(msg.instant+" > "+msg.text);
		}

	}

}
