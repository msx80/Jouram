package com.github.msx80.jouram.core;

import java.lang.reflect.Method;

public interface InstanceController {

	Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

	void commandSync();

	void commandStartTransaction();

	void commandEndTransaction();

	void commandSnapshot(long minimalJournalEntry) throws JouramException;

	void commandClose(boolean quick) throws JouramException;

	Object makeReadOnlyProxy();

}