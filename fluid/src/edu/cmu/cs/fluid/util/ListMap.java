package edu.cmu.cs.fluid.util;

import java.util.*;

import org.apache.commons.collections15.keyvalue.AbstractMapEntry;

/**
 * A basic implementation of Map that preserves the order of mappings
 * 
 * @author Edwin
 */
public final class ListMap<K,V> extends AbstractMap<K, V> {	
	private final List<Entry<K,V>> entries = new ArrayList<Entry<K,V>>();
	
	static class MyEntry<K,V> extends AbstractMapEntry<K, V> {
		MyEntry(K key, V value) {
			super(key, value);
		}
	}
	
	@Override
	public V put(K key, V newValue) {
		// Remove previous value
		V oldValue = null;
		Iterator<Entry<K, V>> it = entries.iterator();
		while (it.hasNext()) {
			Entry<K, V> e = it.next();
			if (e.getKey().equals(key)) {
				oldValue = e.getValue();
				it.remove();
				break;
			}
		}
		entries.add(new MyEntry<K,V>(key, newValue));		
		return oldValue;
	}
	
	@Override
	public Set<Entry<K, V>> entrySet() {
		// TODO copy the entries?
		return new ListSet<Entry<K,V>>(entries);
	}

}
