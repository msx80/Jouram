package com.github.msx80.jouram.core.map;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.msx80.jouram.Jouram;
import com.github.msx80.jouram.core.JouramBuilder;
import com.github.msx80.jouram.core.Mutator;
import com.github.msx80.jouram.core.fs.VFile;

public interface JouramMap<K, V> {

	int size();

	boolean isEmpty();

	boolean containsKey(Object key);

	boolean containsValue(Object value);

	V get(Object key);

	@Mutator
	V put(K key, V value);

	@Mutator
	V remove(Object key);

	@Mutator
	void putAll(Map<? extends K, ? extends V> m);

	@Mutator
	void clear();

	Set<K> keySet();

	Collection<V> values();

	Set<Entry<K, V>> entrySet();


}