package edu.cmu.cs.fluid.util;

//GenericsNote: Converted -- However, null keys will now be represented in the internal structures, a big change.
/*
 *  Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
//package org.apache.commons.collections15.map;

import org.apache.commons.collections15.KeyValue;
import org.apache.commons.collections15.MapIterator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import com.surelogic.Starts;
import com.surelogic.common.util.EmptyIterator;

/**
 * Originally AbstractHashedMap from Commons Collections (generic), but modified
 * to directly use the hashCode() of the key. - No caching of the hash in the
 * map entry.
 * <p>
 * The computation should be pretty quick to call <tt>hash</tt>. In 2012 we
 * stopped pre-computing this in {@link AbstractIRNode} to avoid problems with
 * other collections.
 * 
 * @author java util HashMap
 * @author Matt Hall, John Watkinson, Stephen Colebourne
 * @author Edwin Chan, Tim Halloran
 * @version $Revision: 1.13 $ $Date: 2007/07/19 16:53:35 $
 * @since Commons Collections 3.0
 */
public class CustomizableHashCodeMap<K, V> extends AbstractHashMap<K, V> {

	/**
	 * Map entries
	 */
	protected transient HashEntry<K, V>[] data;
	/**
	 * Entry set
	 */
	protected transient EntrySet<K, V> entrySet;
	/**
	 * Key set
	 */
	protected transient KeySet<K, V> keySet;
	/**
	 * Values
	 */
	protected transient Values<K, V> values;

	/**
	 * Constructor only used in deserialization, do not use otherwise.
	 */
	@SuppressWarnings("unused")
	private CustomizableHashCodeMap() {
		super();
	}

	/**
	 * Constructor which performs no validation on the passed in parameters.
	 * 
	 * @param initialCapacity
	 *            the initial capacity, must be a power of two
	 * @param loadFactor
	 *            the load factor, must be &gt; 0.0f and generally &lt; 1.0f
	 * @param threshold
	 *            the threshold, must be sensible
	 */
	@SuppressWarnings("unchecked")
	protected CustomizableHashCodeMap(int initialCapacity, float loadFactor,
			int threshold) {
		super();
		this.loadFactor = loadFactor;
		this.data = new HashEntry[initialCapacity];
		this.threshold = threshold;
		init();
	}

	/**
	 * Constructs a new, empty map with the specified initial capacity and
	 * default load factor.
	 * 
	 * @param initialCapacity
	 *            the initial capacity
	 * @throws IllegalArgumentException
	 *             if the initial capacity is less than one
	 */
	protected CustomizableHashCodeMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs a new, empty map with the specified initial capacity and load
	 * factor.
	 * 
	 * @param initialCapacity
	 *            the initial capacity
	 * @param loadFactor
	 *            the load factor
	 * @throws IllegalArgumentException
	 *             if the initial capacity is less than one
	 * @throws IllegalArgumentException
	 *             if the load factor is less than or equal to zero
	 */
	@SuppressWarnings("unchecked")
	protected CustomizableHashCodeMap(int initialCapacity, float loadFactor) {
		super();
		if (initialCapacity < 1) {
			throw new IllegalArgumentException(
					"Initial capacity must be greater than 0");
		}
		if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException(
					"Load factor must be greater than 0");
		}
		this.loadFactor = loadFactor;
		this.threshold = calculateThreshold(initialCapacity, loadFactor);
		initialCapacity = calculateNewCapacity(initialCapacity);
		this.data = new HashEntry[initialCapacity];
		init();
	}

	/**
	 * Constructor copying elements from another map.
	 * 
	 * @param map
	 *            the map to copy
	 * @throws NullPointerException
	 *             if the map is null
	 */
	protected CustomizableHashCodeMap(Map<? extends K, ? extends V> map) {
		this(Math.max(2 * map.size(), DEFAULT_CAPACITY), DEFAULT_LOAD_FACTOR);
		putAll(map);
	}

	/**
	 * Gets the value mapped to the key specified.
	 * 
	 * @param key
	 *            the key
	 * @return the mapped value, null if no match
	 */
	@Starts("nothing")
	public V get(Object key) {
		int hashCode = key == null ? NULL_HASH : hash(key.hashCode());
		HashEntry<K, V> entry = data[hashIndex(hashCode, data.length)];

		// case 0
		if (entry == null) {
			return null;
		}
		// case 1
		if (hash(entry.key.hashCode()) == hashCode
				&& isEqualKey(key, entry.key)) {
			return entry.getValue();
		}
		// case 2+
		entry = entry.next;

		while (entry != null) {
			// iters++;
			if (hash(entry.key.hashCode()) == hashCode
					&& isEqualKey(key, entry.key)) {
				return entry.getValue();
			}
			entry = entry.next;
		}
		return null;
	}

	/**
	 * Checks whether the map contains the specified key.
	 * 
	 * @param key
	 *            the key to search for
	 * @return true if the map contains the key
	 */
	@Starts("nothing")
	public boolean containsKey(Object key) {
		int hashCode = key == null ? NULL_HASH : hash(key.hashCode());
		HashEntry<K, V> entry = data[hashIndex(hashCode, data.length)];
		while (entry != null) {
			if (hash(entry.key.hashCode()) == hashCode
					&& isEqualKey(key, entry.getKey())) {
				return true;
			}
			entry = entry.next;
		}
		return false;
	}

	/**
	 * Checks whether the map contains the specified value.
	 * 
	 * @param value
	 *            the value to search for
	 * @return true if the map contains the value
	 */
	@Starts("nothing")
	public boolean containsValue(Object value) {
		if (value == null) {
			for (int i = 0, isize = data.length; i < isize; i++) {
				HashEntry<K, V> entry = data[i];
				while (entry != null) {
					if (entry.getValue() == null) {
						return true;
					}
					entry = entry.next;
				}
			}
		} else {
			for (int i = 0, isize = data.length; i < isize; i++) {
				HashEntry<K, V> entry = data[i];
				while (entry != null) {
					if (isEqualValue(value, entry.getValue())) {
						return true;
					}
					entry = entry.next;
				}
			}
		}
		return false;
	}

	/**
	 * Puts a key-value mapping into this map.
	 * 
	 * @param key
	 *            the key to add
	 * @param value
	 *            the value to add
	 * @return the value previously mapped to this key, null if none
	 */
	public V put(K key, V value) {
		int hashCode = key == null ? NULL_HASH : hash(key.hashCode());
		int index = hashIndex(hashCode, data.length);
		HashEntry<K, V> entry = data[index];
		while (entry != null) {
			if (hash(entry.key.hashCode()) == hashCode
					&& isEqualKey(key, entry.getKey())) {
				V oldValue = entry.getValue();
				updateEntry(entry, value);
				return oldValue;
			}
			entry = entry.next;
		}
		addMapping(index, key, value);
		return null;
	}

	/**
	 * Puts all the values from the specified map into this map.
	 * <p/>
	 * This implementation iterates around the specified map and uses
	 * {@link #put(Object, Object)}.
	 * 
	 * @param map
	 *            the map to add
	 * @throws NullPointerException
	 *             if the map is null
	 */
	public void putAll(Map<? extends K, ? extends V> map) {
		int mapSize = map.size();
		if (mapSize == 0) {
			return;
		}
		int newSize = (int) ((size + mapSize) / loadFactor + 1);
		ensureCapacity(calculateNewCapacity(newSize));

		for (Map.Entry<? extends K, ? extends V> e : map.entrySet())
			put(e.getKey(), e.getValue());
	}

	/**
	 * Removes the specified mapping from this map.
	 * 
	 * @param key
	 *            the mapping to remove
	 * @return the value mapped to the removed key, null if key not in map
	 */
	@Starts("nothing")
	public V remove(Object key) {
		int hashCode = key == null ? NULL_HASH : hash(key.hashCode());
		int index = hashIndex(hashCode, data.length);
		HashEntry<K, V> entry = data[index];
		HashEntry<K, V> previous = null;
		while (entry != null) {
			if (hash(entry.key.hashCode()) == hashCode
					&& isEqualKey(key, entry.getKey())) {
				V oldValue = entry.getValue();
				removeMapping(entry, index, previous);
				return oldValue;
			}
			previous = entry;
			entry = entry.next;
		}
		return null;
	}

	/**
	 * Clears the map, resetting the size to zero and nullifying references to
	 * avoid garbage collection issues.
	 */
	@Starts("nothing")
	public void clear() {
		modCount++;
		HashEntry<K, V>[] data = this.data;
		for (int i = data.length - 1; i >= 0; i--) {
			data[i] = null;
		}
		size = 0;
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because HashMap
	 * uses power-of-two length hash tables, that otherwise encounter collisions
	 * for hashCodes that do not differ in lower bits. Note: Null keys always
	 * map to hash 0, thus index 0.
	 */
	static int hash(int h) {
		// This function ensures that hashCodes that differ only by
		// constant multiples at each bit position have a bounded
		// number of collisions (approximately 8 at default load factor).
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	/**
	 * Gets the entry mapped to the key specified.
	 * <p/>
	 * This method exists for subclasses that may need to perform a multi-step
	 * process accessing the entry. The public methods in this class don't use
	 * this method to gain a small performance boost.
	 * 
	 * @param key
	 *            the key
	 * @return the entry, null if no match
	 */
	public HashEntry<K, V> getEntry(Object key) {
		int hashCode = key == null ? NULL_HASH : hash(key.hashCode());
		HashEntry<K, V> entry = data[hashIndex(hashCode, data.length)];
		while (entry != null) {
			if (hash(entry.key.hashCode()) == hashCode
					&& isEqualKey(key, entry.getKey())) {
				return entry;
			}
			entry = entry.next;
		}
		return null;
	}

	/**
	 * Updates an existing key-value mapping to change the value.
	 * <p/>
	 * This implementation calls <code>setValue()</code> on the entry.
	 * Subclasses could override to handle changes to the map.
	 * 
	 * @param entry
	 *            the entry to update
	 * @param newValue
	 *            the new value to store
	 */
	protected void updateEntry(HashEntry<K, V> entry, V newValue) {
		entry.setValue(newValue);
	}

	/**
	 * Reuses an existing key-value mapping, storing completely new data.
	 * <p/>
	 * This implementation sets all the data fields on the entry. Subclasses
	 * could populate additional entry fields.
	 * 
	 * @param entry
	 *            the entry to update, not null
	 * @param hashIndex
	 *            the index in the data array
	 * @param hashCode
	 *            the hash code of the key to add
	 * @param key
	 *            the key to add
	 * @param value
	 *            the value to add
	 */
	protected void reuseEntry(HashEntry<K, V> entry, int hashIndex,
			int hashCode, K key, V value) {
		entry.next = data[hashIndex];
		entry.key = key;
		entry.value = value;
	}

	/**
	 * Adds a new key-value mapping into this map.
	 * <p/>
	 * This implementation calls <code>createEntry()</code>,
	 * <code>addEntry()</code> and <code>checkCapacity()</code>. It also handles
	 * changes to <code>modCount</code> and <code>size</code>. Subclasses could
	 * override to fully control adds to the map.
	 * 
	 * @param hashIndex
	 *            the index into the data array to store at
	 * @param hashCode
	 *            the hash code of the key to add
	 * @param key
	 *            the key to add
	 * @param value
	 *            the value to add
	 */
	protected void addMapping(int hashIndex, K key, V value) {
		modCount++;
		HashEntry<K, V> entry = createEntry(data[hashIndex], key, value);
		addEntry(entry, hashIndex);
		size++;
		checkCapacity();
	}

	/**
	 * Creates an entry to store the key-value data.
	 * <p/>
	 * This implementation creates a new HashEntry instance. Subclasses can
	 * override this to return a different storage class, or implement caching.
	 * 
	 * @param next
	 *            the next entry in sequence
	 * @param hashCode
	 *            the hash code to use
	 * @param key
	 *            the key to store
	 * @param value
	 *            the value to store
	 * @return the newly created entry
	 */
	protected HashEntry<K, V> createEntry(HashEntry<K, V> next, K key, V value) {
		return new HashEntry<K, V>(next, key, value);
	}

	/**
	 * Adds an entry into this map.
	 * <p/>
	 * This implementation adds the entry to the data storage table. Subclasses
	 * could override to handle changes to the map.
	 * 
	 * @param entry
	 *            the entry to add
	 * @param hashIndex
	 *            the index into the data array to store at
	 */
	protected void addEntry(HashEntry<K, V> entry, int hashIndex) {
		data[hashIndex] = entry;
	}

	/**
	 * Removes a mapping from the map.
	 * <p/>
	 * This implementation calls <code>removeEntry()</code> and
	 * <code>destroyEntry()</code>. It also handles changes to
	 * <code>modCount</code> and <code>size</code>. Subclasses could override to
	 * fully control removals from the map.
	 * 
	 * @param entry
	 *            the entry to remove
	 * @param hashIndex
	 *            the index into the data structure
	 * @param previous
	 *            the previous entry in the chain
	 */
	protected void removeMapping(HashEntry<K, V> entry, int hashIndex,
			HashEntry<K, V> previous) {
		modCount++;
		removeEntry(entry, hashIndex, previous);
		size--;
		destroyEntry(entry);
	}

	/**
	 * Removes an entry from the chain stored in a particular index.
	 * <p/>
	 * This implementation removes the entry from the data storage table. The
	 * size is not updated. Subclasses could override to handle changes to the
	 * map.
	 * 
	 * @param entry
	 *            the entry to remove
	 * @param hashIndex
	 *            the index into the data structure
	 * @param previous
	 *            the previous entry in the chain
	 */
	protected void removeEntry(HashEntry<K, V> entry, int hashIndex,
			HashEntry<K, V> previous) {
		if (previous == null) {
			data[hashIndex] = entry.next;
		} else {
			previous.next = entry.next;
		}
	}

	/**
	 * Kills an entry ready for the garbage collector.
	 * <p/>
	 * This implementation prepares the HashEntry for garbage collection.
	 * Subclasses can override this to implement caching (override clear as
	 * well).
	 * 
	 * @param entry
	 *            the entry to destroy
	 */
	protected void destroyEntry(HashEntry<K, V> entry) {
		entry.next = null;
		entry.key = null;
		entry.value = null;
	}

	/**
	 * Checks the capacity of the map and enlarges it if necessary.
	 * <p/>
	 * This implementation uses the threshold to check if the map needs
	 * enlarging
	 */
	protected void checkCapacity() {
		if (size >= threshold) {
			int newCapacity = data.length * 2;
			if (newCapacity <= MAXIMUM_CAPACITY) {
				ensureCapacity(newCapacity);
			}
		}
	}

	/**
	 * Changes the size of the data structure to the capacity proposed.
	 * 
	 * @param newCapacity
	 *            the new capacity of the array (a power of two, less or equal
	 *            to max)
	 */

	protected void ensureCapacity(int newCapacity) {
		int oldCapacity = data.length;
		if (newCapacity <= oldCapacity) {
			return;
		}
		resize(newCapacity);
	}

	/**
	 * Make the table smaller if possible
	 * 
	 * @return true if compacted
	 */
	protected boolean compact() {
		int currentCapacity = data.length;
		int newCapacity = currentCapacity >>> 1;
		while (size < calculateThreshold(newCapacity, loadFactor)) {
			currentCapacity = newCapacity;
			newCapacity = currentCapacity >>> 1;
		}
		if (currentCapacity < data.length) {
			resize(currentCapacity);
			return true;
		}
		return false;
	}

	/**
	 * Changes the size of the data structure to the capacity proposed.
	 * 
	 * @param newCapacity
	 *            the new capacity of the array (a power of two, less or equal
	 *            to max)
	 */
	@SuppressWarnings("unchecked")
	private void resize(int newCapacity) {
		int oldCapacity = data.length;

		if (size == 0) {
			threshold = calculateThreshold(newCapacity, loadFactor);
			data = new HashEntry[newCapacity];
		} else {
			HashEntry<K, V> oldEntries[] = data;
			HashEntry<K, V> newEntries[] = new HashEntry[newCapacity];

			modCount++;
			int newSize = size;
			for (int i = oldCapacity - 1; i >= 0; i--) {
				HashEntry<K, V> entry = oldEntries[i];
				// process chain of entries
				if (entry != null) {
					oldEntries[i] = null; // gc
					do {
						HashEntry<K, V> next = entry.next;
						if (isValidEntry(entry)) {
							int index = hashIndex(hash(entry.key.hashCode()),
									newCapacity);
							entry.next = newEntries[index];
							newEntries[index] = entry;
						} else {
							newSize--;
						}
						entry = next;
					} while (entry != null);
				}
			}
			threshold = calculateThreshold(newCapacity, loadFactor);
			data = newEntries;
			size = newSize;
		}
	}

	/**
	 * Used while cleaning up
	 */
	protected boolean isValidEntry(HashEntry<K, V> e) {
		return true;
	}

	/**
	 * @return Number of entries cleaned up
	 */
	protected int cleanup() {
		if (size == 0) {
			return 0; // nothing to do
		}
		int removed = 0;

		// This low-level loop is necessary because the iterator.remove()
		// method uses the high-level access, rather than directly removing the
		// entry. But destroyed nodes cannot be found under their old IDs.
		for (int i = 0; i < data.length; ++i) {
			HashEntry<K, V> e = data[i], last = null, next;
			while (e != null) {
				next = e.next;
				if (isValidEntry(e)) {
					// valid, so just skip it
					last = e;
				} else {
					// invalid entry, so remove it
					if (last == null) {
						data[i] = next;
					} else {
						last.next = next;
					}
					++removed;
					--size;
				}
				e = next;
			}
		}
		return removed;
	}

	/**
	 * Gets an iterator over the map. Changes made to the iterator affect this
	 * map.
	 * <p/>
	 * A MapIterator returns the keys in the map. It also provides convenient
	 * methods to get the key and value, and set the value. It avoids the need
	 * to create an entrySet/keySet/values object. It also avoids creating the
	 * Map.Entry object.
	 * 
	 * @return the map iterator
	 */
	public MapIterator<K, V> mapIterator() {
		if (size == 0) {
			return EmptyMapIterator.prototype();
		}
		return new HashMapIterator<K, V>(this);
	}

	/**
	 * MapIterator implementation.
	 */
	protected static class HashMapIterator<K, V> extends HashIterator<K, V>
			implements MapIterator<K, V> {

		protected HashMapIterator(CustomizableHashCodeMap<K, V> parent) {
			super(parent);
		}

		public K next() {
			return super.nextEntry().getKey();
		}

		public K getKey() {
			HashEntry<K, V> current = currentEntry();
			if (current == null) {
				throw new IllegalStateException(AbstractHashMap.GETKEY_INVALID);
			}
			return current.getKey();
		}

		public V getValue() {
			HashEntry<K, V> current = currentEntry();
			if (current == null) {
				throw new IllegalStateException(
						AbstractHashMap.GETVALUE_INVALID);
			}
			return current.getValue();
		}

		public V setValue(V value) {
			HashEntry<K, V> current = currentEntry();
			if (current == null) {
				throw new IllegalStateException(
						AbstractHashMap.SETVALUE_INVALID);
			}
			return current.setValue(value);
		}
	}

	/**
	 * Gets the entrySet view of the map. Changes made to the view affect this
	 * map. To simply iterate through the entries, use {@link #mapIterator()}.
	 * 
	 * @return the entrySet view
	 */
	@Starts("nothing")
	public Set<Map.Entry<K, V>> entrySet() {
		if (entrySet == null) {
			entrySet = new EntrySet<K, V>(this);
		}
		return entrySet;
	}

	/**
	 * Creates an entry set iterator. Subclasses can override this to return
	 * iterators with different properties.
	 * 
	 * @return the entrySet iterator
	 */
	protected Iterator<Map.Entry<K, V>> createEntrySetIterator() {
		if (size() == 0) {
			return new EmptyIterator<Entry<K, V>>();
		}
		return new EntrySetIterator<K, V>(this);
	}

	/**
	 * EntrySet implementation.
	 */
	protected static class EntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {
		/**
		 * The parent map
		 */
		protected final CustomizableHashCodeMap<K, V> parent;

		protected EntrySet(CustomizableHashCodeMap<K, V> parent) {
			super();
			this.parent = parent;
		}

		@Starts("nothing")
		@Override
		public int size() {
			return parent.size();
		}

		@Starts("nothing")
		@Override
		public void clear() {
			parent.clear();
		}

		public boolean contains(Map.Entry<K, V> entry) {
			Map.Entry<K, V> e = entry;
			Entry<K, V> match = parent.getEntry(e.getKey());
			return (match != null && match.equals(e));
		}

		@Starts("nothing")
		@Override
		@SuppressWarnings("unchecked")
		public boolean remove(Object obj) {
			if (obj instanceof Map.Entry == false) {
				return false;
			}
			if (contains(obj) == false) {
				return false;
			}
			Map.Entry<K, V> entry = (Map.Entry<K, V>) obj;
			K key = entry.getKey();
			parent.remove(key);
			return true;
		}

		@Starts("nothing")
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return parent.createEntrySetIterator();
		}
	}

	/**
	 * EntrySet iterator.
	 */
	protected static class EntrySetIterator<K, V> extends HashIterator<K, V>
			implements Iterator<Map.Entry<K, V>> {

		protected EntrySetIterator(CustomizableHashCodeMap<K, V> parent) {
			super(parent);
		}

		public HashEntry<K, V> next() {
			return super.nextEntry();
		}
	}

	/**
	 * Gets the keySet view of the map. Changes made to the view affect this
	 * map. To simply iterate through the keys, use {@link #mapIterator()}.
	 * 
	 * @return the keySet view
	 */
	@Starts("nothing")
	public Set<K> keySet() {
		if (keySet == null) {
			keySet = new KeySet<K, V>(this);
		}
		return keySet;
	}

	/**
	 * Creates a key set iterator. Subclasses can override this to return
	 * iterators with different properties.
	 * 
	 * @return the keySet iterator
	 */
	protected Iterator<K> createKeySetIterator() {
		if (size() == 0) {
			return new EmptyIterator<K>();
		}
		return new KeySetIterator<K, V>(this);
	}

	/**
	 * KeySet implementation.
	 */
	protected static class KeySet<K, V> extends AbstractSet<K> {
		/**
		 * The parent map
		 */
		protected final CustomizableHashCodeMap<K, V> parent;

		protected KeySet(CustomizableHashCodeMap<K, V> parent) {
			super();
			this.parent = parent;
		}

		@Starts("nothing")
		@Override
		public int size() {
			return parent.size();
		}

		@Starts("nothing")
		@Override
		public void clear() {
			parent.clear();
		}

		@Starts("nothing")
		@Override
		public boolean contains(Object key) {
			return parent.containsKey(key);
		}

		@Starts("nothing")
		@Override
		public boolean remove(Object key) {
			boolean result = parent.containsKey(key);
			parent.remove(key);
			return result;
		}

		@Starts("nothing")
		@Override
		public Iterator<K> iterator() {
			return parent.createKeySetIterator();
		}
	}

	/**
	 * KeySet iterator.
	 */
	protected static class KeySetIterator<K, V> extends HashIterator<K, V>
			implements Iterator<K> {

		protected KeySetIterator(CustomizableHashCodeMap<K, V> parent) {
			super(parent);
		}

		public K next() {
			return super.nextEntry().getKey();
		}
	}

	/**
	 * Gets the values view of the map. Changes made to the view affect this
	 * map. To simply iterate through the values, use {@link #mapIterator()}.
	 * 
	 * @return the values view
	 */
	@Starts("nothing")
	public Collection<V> values() {
		if (values == null) {
			values = new Values<K, V>(this);
		}
		return values;
	}

	/**
	 * Creates a values iterator. Subclasses can override this to return
	 * iterators with different properties.
	 * 
	 * @return the values iterator
	 */
	protected Iterator<V> createValuesIterator() {
		if (size() == 0) {
			return new EmptyIterator<V>();
		}
		return new ValuesIterator<K, V>(this);
	}

	/**
	 * Values implementation.
	 */
	protected static class Values<K, V> extends AbstractCollection<V> {
		/**
		 * The parent map
		 */
		protected final CustomizableHashCodeMap<K, V> parent;

		protected Values(CustomizableHashCodeMap<K, V> parent) {
			super();
			this.parent = parent;
		}

		@Starts("nothing")
		@Override
		public int size() {
			return parent.size();
		}

		@Starts("nothing")
		@Override
		public void clear() {
			parent.clear();
		}

		@Starts("nothing")
		@Override
		public boolean contains(Object value) {
			return parent.containsValue(value);
		}

		@Starts("nothing")
		@Override
		public Iterator<V> iterator() {
			return parent.createValuesIterator();
		}
	}

	/**
	 * Values iterator.
	 */
	protected static class ValuesIterator<K, V> extends HashIterator<K, V>
			implements Iterator<V> {

		protected ValuesIterator(CustomizableHashCodeMap<K, V> parent) {
			super(parent);
		}

		public V next() {
			return super.nextEntry().getValue();
		}
	}

	/**
	 * HashEntry used to store the data.
	 * <p/>
	 * If you subclass <code>AbstractHashedMap</code> but not
	 * <code>HashEntry</code> then you will not be able to access the protected
	 * fields. The <code>entryXxx()</code> methods on
	 * <code>AbstractHashedMap</code> exist to provide the necessary access.
	 */
	protected static class HashEntry<K, V> implements Map.Entry<K, V>,
			KeyValue<K, V> {
		/**
		 * The next entry in the hash chain
		 */
		protected HashEntry<K, V> next;

		/**
		 * The key
		 */
		private K key;
		/**
		 * The value
		 */
		private V value;

		protected HashEntry(HashEntry<K, V> next, K key, V value) {
			super();
			this.next = next;
			this.key = key;
			this.value = value;
		}

		@Starts("nothing")
		public K getKey() {
			return key;
		}

		public void setKey(K key) {
			this.key = key;
		}

		@Starts("nothing")
		public V getValue() {
			return value;
		}

		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Starts("nothing")
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof Map.Entry == false) {
				return false;
			}
			@SuppressWarnings("unchecked")
			Map.Entry<K, V> other = (Map.Entry<K, V>) obj;
			return (getKey() == null ? other.getKey() == null : getKey()
					.equals(other.getKey()))
					&& (getValue() == null ? other.getValue() == null
							: getValue().equals(other.getValue()));
		}

		@Starts("nothing")
		@Override
		public int hashCode() {
			return (getKey() == null ? 0 : getKey().hashCode())
					^ (getValue() == null ? 0 : getValue().hashCode());
		}

		@Override
		public String toString() {
			return new StringBuilder().append(getKey()).append('=')
					.append(getValue()).toString();
		}
	}

	/**
	 * Base Iterator
	 */
	protected static abstract class HashIterator<K, V> {

		/**
		 * The parent map
		 */
		protected final CustomizableHashCodeMap<K, V> parent;
		/**
		 * The current index into the array of buckets
		 */
		protected int hashIndex;
		/**
		 * The last returned entry
		 */
		protected HashEntry<K, V> last;
		/**
		 * The next entry
		 */
		protected HashEntry<K, V> next;
		/**
		 * The modification count expected
		 */
		protected int expectedModCount;

		protected HashIterator(CustomizableHashCodeMap<K, V> parent) {
			super();
			this.parent = parent;
			HashEntry<K, V>[] data = parent.data;
			int i = data.length;
			HashEntry<K, V> next = null;
			while (i > 0 && next == null) {
				next = data[--i];
			}
			this.next = next;
			this.hashIndex = i;
			this.expectedModCount = parent.modCount;
		}

		public boolean hasNext() {
			return (next != null);
		}

		protected HashEntry<K, V> nextEntry() {
			if (parent.modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			HashEntry<K, V> newCurrent = next;
			if (newCurrent == null) {
				throw new NoSuchElementException(AbstractHashMap.NO_NEXT_ENTRY);
			}
			HashEntry<K, V>[] data = parent.data;
			int i = hashIndex;
			HashEntry<K, V> n = newCurrent.next;
			while (n == null && i > 0) {
				n = data[--i];
			}
			next = n;
			hashIndex = i;
			last = newCurrent;
			return newCurrent;
		}

		protected HashEntry<K, V> currentEntry() {
			return last;
		}

		public void remove() {
			if (last == null) {
				throw new IllegalStateException(AbstractHashMap.REMOVE_INVALID);
			}
			if (parent.modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			parent.remove(last.getKey());
			last = null;
			expectedModCount = parent.modCount;
		}

		@Override
		public String toString() {
			if (last != null) {
				return "Iterator[" + last.getKey() + "=" + last.getValue()
						+ "]";
			} else {
				return "Iterator[]";
			}
		}
	}

	/**
	 * Writes the map data to the stream. This method must be overridden if a
	 * subclass must be setup before <code>put()</code> is used.
	 * <p/>
	 * Serialization is not one of the JDK's nicest topics. Normal serialization
	 * will initialise the superclass before the subclass. Sometimes however,
	 * this isn't what you want, as in this case the <code>put()</code> method
	 * on read can be affected by subclass state.
	 * <p/>
	 * The solution adopted here is to serialize the state data of this class in
	 * this protected method. This method must be called by the
	 * <code>writeObject()</code> of the first serializable subclass.
	 * <p/>
	 * Subclasses may override if they have a specific field that must be
	 * present on read before this implementation will work. Generally, the read
	 * determines what must be serialized here, if anything.
	 * 
	 * @param out
	 *            the output stream
	 */
	protected void doWriteObject(ObjectOutputStream out) throws IOException {
		out.writeFloat(loadFactor);
		out.writeInt(data.length);
		out.writeInt(size);
		for (MapIterator<K, V> it = mapIterator(); it.hasNext();) {
			out.writeObject(it.next());
			out.writeObject(it.getValue());
		}
	}

	/**
	 * Reads the map data from the stream. This method must be overridden if a
	 * subclass must be setup before <code>put()</code> is used.
	 * <p/>
	 * Serialization is not one of the JDK's nicest topics. Normal serialization
	 * will initialise the superclass before the subclass. Sometimes however,
	 * this isn't what you want, as in this case the <code>put()</code> method
	 * on read can be affected by subclass state.
	 * <p/>
	 * The solution adopted here is to deserialize the state data of this class
	 * in this protected method. This method must be called by the
	 * <code>readObject()</code> of the first serializable subclass.
	 * <p/>
	 * Subclasses may override if the subclass has a specific field that must be
	 * present before <code>put()</code> or <code>calculateThreshold()</code>
	 * will work correctly.
	 * 
	 * @param in
	 *            the input stream
	 */
	@SuppressWarnings("unchecked")
	protected void doReadObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		loadFactor = in.readFloat();
		int capacity = in.readInt();
		int size = in.readInt();
		init();
		data = new HashEntry[capacity];
		for (int i = 0; i < size; i++) {
			K key = (K) in.readObject();
			V value = (V) in.readObject();
			put(key, value);
		}
		threshold = calculateThreshold(data.length, loadFactor);
	}

	/**
	 * Clones the map without cloning the keys or values.
	 * <p/>
	 * To implement <code>clone()</code>, a subclass must implement the
	 * <code>Cloneable</code> interface and make this method public.
	 * 
	 * @return a shallow clone
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object clone() {
		try {
			CustomizableHashCodeMap<K, V> cloned = (CustomizableHashCodeMap<K, V>) super
					.clone();
			cloned.data = (HashEntry<K, V>[]) new HashEntry[data.length];
			cloned.entrySet = null;
			cloned.keySet = null;
			cloned.values = null;
			cloned.modCount = 0;
			cloned.size = 0;
			cloned.init();
			cloned.putAll(this);
			return cloned;

		} catch (CloneNotSupportedException ex) {
			return null; // should never happen
		}
	}

	/**
	 * Compares this map with another.
	 * 
	 * @param obj
	 *            the object to compare to
	 * @return true if equal
	 */
	@Starts("nothing")
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Map == false) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Map<K, V> map = (Map<K, V>) obj;
		if (map.size() != size()) {
			return false;
		}
		MapIterator<K, V> it = mapIterator();
		try {
			while (it.hasNext()) {
				Object key = it.next();
				Object value = it.getValue();
				if (value == null) {
					if (map.get(key) != null || map.containsKey(key) == false) {
						return false;
					}
				} else {
					if (value.equals(map.get(key)) == false) {
						return false;
					}
				}
			}
		} catch (ClassCastException ignored) {
			return false;
		} catch (NullPointerException ignored) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the standard Map hashCode.
	 * 
	 * @return the hash code defined in the Map interface
	 */
	@Starts("nothing")
	@Override
	public int hashCode() {
		int total = 0;
		final Iterator<Entry<K, V>> it = createEntrySetIterator();
		while (it.hasNext()) {
			total += it.next().hashCode();
		}
		return total;
	}

	/**
	 * Gets the map as a String.
	 * 
	 * @return a string version of the map
	 */
	@Override
	public String toString() {
		if (size() == 0) {
			return "{}";
		}
		StringBuilder buf = new StringBuilder(32 * size());
		buf.append('{');

		final MapIterator<K, V> it = mapIterator();
		boolean hasNext = it.hasNext();
		while (hasNext) {
			Object key = it.next();
			Object value = it.getValue();
			buf.append(key == this ? "(this Map)" : key).append('=')
					.append(value == this ? "(this Map)" : value);

			hasNext = it.hasNext();
			if (hasNext) {
				buf.append(',').append(' ');
			}
		}

		buf.append('}');
		return buf.toString();
	}

	public static void main(String[] args) {
		class M<K, V> extends CustomizableHashCodeMap<K, V> {
			M() {
				super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
			}
		}
		Map<Object, Object> map = new M<Object, Object>();
		map.put("hi", "bye");
		System.out.println(map.get("hi"));

		/*
		IRNode foo = new MarkedIRNode("foo");
		map.put(foo, "bar");
		System.out.println(map.get(foo));

		String[] names = { "a", "b", "c", "d", "e", "f", "g" };
		IRNode[] nodes = new IRNode[names.length];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new MarkedIRNode(names[i]);
			map.put(nodes[i], nodes[i]);
		}
		for (IRNode n : nodes) {
			System.out.println(map.get(n));
		}
		*/
		System.out.println(map);
	}
}
