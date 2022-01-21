package com.github.msx80.jouram.core.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.github.msx80.jouram.core.InstanceController;
import com.github.msx80.jouram.core.Jouramed;

public final class FrontFacingMap<K, V> implements Map<K, V>, Jouramed 
{

	JouramMap<K,V> d;

	public FrontFacingMap(JouramMap<K, V> d) {
		super();
		this.d = d;
	}

	public int size() {
		return d.size();
	}

	public boolean isEmpty() {
		return d.isEmpty();
	}

	public boolean containsKey(Object key) {
		return d.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return d.containsValue(value);
	}

	public V get(Object key) {
		return d.get(key);
	}

	public V put(K key, V value) {
		return d.put(key, value);
	}

	public V remove(Object key) {
		return d.remove(key);
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		d.putAll(m);
	}

	public void clear() {
		d.clear();
	}

	public Set<K> keySet() {
		return d.keySet();
	}

	public Collection<V> values() {
		return d.values();
	}

	public Set<Entry<K, V>> entrySet() {
		return d.entrySet();
	}

	public boolean equals(Object o) {
		return d.equals(o);
	}

	public int hashCode() {
		return d.hashCode();
	}

	@Override
	public InstanceController getJouram() {
		// trick to let Jouram methods work with this
		return ((Jouramed)d).getJouram();
	}
	
	public String toString()
	{
		return d.toString();
	}
	
}
