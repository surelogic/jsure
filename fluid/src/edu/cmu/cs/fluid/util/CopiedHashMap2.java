/*
 * @(#)HashMap.java	1.57 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 * Changed:
 *  Made table/size protected (for VersionedHashMap)
 *  Exported setNext from Entry
 */

package edu.cmu.cs.fluid.util;
import  java.util.*;
import  java.io.*;
import com.surelogic.Starts;

/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the capacity is roughly doubled by calling the
 * <tt>rehash</tt> method.
 *
 * <p>As a general rule, the default load factor (.75) offers a good tradeoff
 * between time and space costs.  Higher values decrease the space overhead
 * but increase the lookup cost (reflected in most of the operations of the
 * <tt>HashMap</tt> class, including <tt>get</tt> and <tt>put</tt>).  The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the
 * number of <tt>rehash</tt> operations.  If the initial capacity is greater
 * than the maximum number of entries divided by the load factor, no
 * <tt>rehash</tt> operations will ever occur.
 *
 * <p>If many mappings are to be stored in a <tt>HashMap</tt> instance,
 * creating it with a sufficiently large capacity will allow the mappings to
 * be stored more efficiently than letting it perform automatic rehashing as
 * needed to grow the table.
 *
 * <p><b>Note that this implementation is not synchronized.</b> If multiple
 * threads access this map concurrently, and at least one of the threads
 * modifies the map structurally, it <i>must</i> be synchronized externally.
 * (A structural modification is any operation that adds or deletes one or
 * more mappings; merely changing the value associated with a key that an
 * instance already contains is not a structural modification.)  This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.  If no such object exists, the map should be
 * "wrapped" using the <tt>Collections.synchronizedMap</tt> method.  This is
 * best done at creation time, to prevent accidental unsynchronized access to
 * the map: <pre> Map m = Collections.synchronizedMap(new HashMap(...));
 * </pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> or <tt>add</tt> methods, the iterator will throw a
 * <tt>ConcurrentModificationException</tt>.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis. 
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the 
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @version 1.57, 01/23/03
 * @see     Object#hashCode()
 * @see     Collection
 * @see	    Map
 * @see	    TreeMap
 * @see	    Hashtable
 * @since   1.2
 */
@SuppressWarnings("all")
public class CopiedHashMap2 extends AbstractMap implements Map, Cloneable,
    Serializable
{
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     **/
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    transient protected IHashEntry[] table;

    /**
     * The number of key-value mappings contained in this identity hash map.
     */
    transient protected int size;
  
    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    int threshold;
  
    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient volatile int modCount;
    
  	transient volatile Set        keySet = null;
	  transient volatile Collection values = null;

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity The initial capacity.
     * @param  loadFactor      The load factor.
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive.
     */
    public CopiedHashMap2(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity) 
            capacity <<= 1;
    
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        table = new IHashEntry[capacity];
        init();
    }
  
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public CopiedHashMap2(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public CopiedHashMap2() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new IHashEntry[DEFAULT_INITIAL_CAPACITY];
        init();
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    public CopiedHashMap2(Map m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                      DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        putAllForCreate(m);
    }

    // internal utilities

    /**
     * Initialization hook for subclasses. This method is called
     * in all constructors and pseudo-constructors (clone, readObject)
     * after HashMap has been initialized but before any entries have
     * been inserted.  (In the absence of this method, readObject would
     * require explicit knowledge of subclasses.)
     */
    void init() {
    }

    /**
     * Value representing null keys inside tables.
     */
    protected static final Object NULL_KEY = new Object();

    /**
     * Returns internal representation for key. Use NULL_KEY if key is null.
     */
    protected static Object maskNull(Object key) {
        return (key == null ? NULL_KEY : key);
    }

    /**
     * Returns key represented by specified internal representation.
     */
    static Object unmaskNull(Object key) {
        return (key == NULL_KEY ? null : key);
    }

    /**
     * Returns a hash value for the specified object.  In addition to 
     * the object's own hashCode, this method applies a "supplemental
     * hash function," which defends against poor quality hash functions.
     * This is critical because HashMap uses power-of two length 
     * hash tables.<p>
     *
     * The shift distances in this function were chosen as the result
     * of an automated search over the entire four-dimensional search space.
     */
    public static int hash(Object x) {
        int h = x.hashCode();

        h += ~(h << 9);
        h ^=  (h >>> 14);
        h +=  (h << 4);
        h ^=  (h >>> 10);
        return h;
    }

    /** 
     * Check for equality of non-null reference x and possibly-null y. 
     */
    static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * Returns index for hash code h. 
     */
    protected static int indexFor(int h, int length) {
        return h & (length-1);
    }
 
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    @Starts("nothing")
	@Override
    public int size() {
        return size;
    }
  
    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    @Starts("nothing")
	@Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped in this identity
     * hash map, or <tt>null</tt> if the map contains no mapping for this key.
     * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contains no mapping for the key; it is also possible that
     * the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two cases.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    @Starts("nothing")
	@Override
    public Object get(Object key) {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        IHashEntry e = table[i]; 
        while (true) {
            if (e == null)
                return e;
            if (e.getHash() == hash && eq(k, e.getKey())) 
                return e.getValue();
            e = e.getNext();
        }
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    @Starts("nothing")
	@Override
    public boolean containsKey(Object key) {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        IHashEntry e = table[i]; 
        while (e != null) {
            if (e.getHash() == hash && eq(k, e.getKey())) 
                return true;
            e = e.getNext();
        }
        return false;
    }

    /**
     * Returns the entry associated with the specified key in the
     * HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    IHashEntry getEntry0(Object key) {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        IHashEntry e = table[i]; 
        while (e != null && !(e.getHash() == hash && eq(k, e.getKey())))
            e = e.getNext();
        return e;
    }
  
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the HashMap previously associated
     *	       <tt>null</tt> with the specified key.
     */
    @Override
    public Object put(Object key, Object value) {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);

        for (IHashEntry e = table[i]; e != null; e = e.getNext()) {
            if (e.getHash() == hash && eq(k, e.getKey())) {
                Object oldValue = e.getValue();
                e.setValue(value);
                //e.recordAccess(this);
                return oldValue;
            }
        }

        modCount++;
        addEntry(hash, k, value, i);
        return null;
    }

    /**
     * This method is used instead of put by constructors and
     * pseudoconstructors (clone, readObject).  It does not resize the table,
     * check for comodification, etc.  It calls createEntry rather than
     * addEntry.
     */
    private void putForCreate(Object key, Object value) {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);

        /**
         * Look for preexisting entry for key.  This will never happen for
         * clone or deserialize.  It will only happen for construction if the
         * input Map is a sorted map whose ordering is inconsistent w/ equals.
         */
        for (IHashEntry e = table[i]; e != null; e = e.getNext()) {
            if (e.getHash() == hash && eq(k, e.getKey())) {
                e.setValue(value);
                return;
            }
        }

        createEntry(hash, k, value, i);
    }

    void putAllForCreate(Map m) {
        for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            putForCreate(e.getKey(), e.getValue());
        }
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    protected void resize(int newCapacity) {
        IHashEntry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        IHashEntry[] newTable = new IHashEntry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * loadFactor);
    }

    /** 
     * Transfer all entries from current table to newTable.
     */
    void transfer(IHashEntry[] newTable) {
        IHashEntry[] src = table;
        int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++) {
            IHashEntry e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                    IHashEntry next = e.getNext();
                    int i = indexFor(e.getHash(), newCapacity);  
                    e.setNext(newTable[i]);
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * These mappings will replace any mappings that
     * this map had for any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map.
     * @throws NullPointerException if the specified map is null.
     */
    @Override
    public void putAll(Map m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            put(e.getKey(), e.getValue());
        }
    }
  
    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key.
     */
    @Starts("nothing")
	@Override
    public Object remove(Object key) {
        IHashEntry e = removeEntryForKey(key);
        return (e == null ? e : e.getValue());
    }

    /**
     * Removes and returns the entry associated with the specified key
     * in the HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    IHashEntry removeEntryForKey(Object key) {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        IHashEntry prev = table[i];
        IHashEntry e = prev;

        while (e != null) {
            IHashEntry next = e.getNext();
            if (e.getHash() == hash && eq(k, e.getKey())) {
                modCount++;
                size--;
                if (prev == e) 
                    table[i] = next;
                else
                    prev.setNext(next);
                //e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }
   
        return e;
    }

    /**
     * Special version of remove for EntrySet.
     */
    IHashEntry removeMapping(Object o) {
        if (!(o instanceof Map.Entry))
            return null;

        Map.Entry entry = (Map.Entry)o;
        Object k = maskNull(entry.getKey());
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        IHashEntry prev = table[i];
        IHashEntry e = prev;

        while (e != null) {
            IHashEntry next = e.getNext();
            if (e.getHash() == hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) 
                    table[i] = next;
                else
                    prev.setNext(next);
                //e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }
   
        return e;
    }

    /**
     * Removes all mappings from this map.
     */
    @Starts("nothing")
	@Override
    public void clear() {
        modCount++;
        IHashEntry tab[] = table;
        for (int i = 0; i < tab.length; i++) 
            tab[i] = null;
        size = 0;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    @Starts("nothing")
	@Override
    public boolean containsValue(Object value) {
	if (value == null) 
            return containsNullValue();

	IHashEntry tab[] = table;
        for (int i = 0; i < tab.length ; i++)
            for (IHashEntry e = tab[i] ; e != null ; e = e.getNext())
                if (value.equals(e.getValue()))
                    return true;
	return false;
    }

    /**
     * Special-case code for containsValue with null argument
     **/
    private boolean containsNullValue() {
	IHashEntry tab[] = table;
        for (int i = 0; i < tab.length ; i++)
            for (IHashEntry e = tab[i] ; e != null ; e = e.getNext())
                if (e.getValue() == null)
                    return true;
	return false;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    @Override
    public Object clone() {
			CopiedHashMap2 result = null;
	try { 
	    result = (CopiedHashMap2)super.clone();
		  result.keySet = null;
		  result.values = null;
	} catch (CloneNotSupportedException e) { 
	    // assert false;
	}
        result.table = new IHashEntry[table.length];
        result.entrySet = null;
        result.modCount = 0;
        result.size = 0;
        result.init();
        result.putAllForCreate(this);

        return result;
    }

    public interface IHashEntry extends Map.Entry {
      int getHash();
      IHashEntry getNext();
      void setNext(IHashEntry newNext);
    }
    
    static protected class Entry implements IHashEntry {
        final Object key;
        final int hash;
  			Object value;        
        IHashEntry next;

        Entry(Object k, Object v) {
          this(hash(k), k, v); 
        }
      
        Entry(int h, Object k, Object v) {
          value = v; 
          key = k;
          hash = h;
        }

        /**
         * Create new entry.
         */
        protected Entry(int h, Object k, Object v, IHashEntry n) { 
          this(h, k, v);
          next = n;
        }

        @Starts("nothing")
		public final Object getKey() {
            return unmaskNull(key);
        }

        @Starts("nothing")
		public final Object getValue() {
            return value;
        }
    
        public final IHashEntry getNext() {
          return next;
        }
        
        public final Object setValue(Object newValue) {
            Object oldValue = value;
            value = newValue;
            return oldValue;
        }
        public final void setNext(IHashEntry newNext) {
          next = newNext;
        }
    
        @Starts("nothing")
		@Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                    return true;
            }
            return false;
        }
    
        @Starts("nothing")
		@Override
        public int hashCode() {
            return (key==NULL_KEY ? 0 : key.hashCode()) ^
                   (value==null   ? 0 : value.hashCode());
        }
    
        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }

        /**
         * This method is invoked whenever the value in an entry is
         * overwritten by an invocation of put(k,v) for a key k that's already
         * in the HashMap.
         */
        void recordAccess(CopiedHashMap2 m) {
        }

        /**
         * This method is invoked whenever the entry is
         * removed from the table.
         */
        void recordRemoval(CopiedHashMap2 m) {
        }

        public int getHash() {
          return hash;
        }
    }

    /**
     * Add a new entry with the specified key, value and hash code to
     * the specified bucket.  It is the responsibility of this 
     * method to resize the table if appropriate.
     *
     * Subclass overrides this to alter the behavior of put method.
     */
    void addEntry(int hash, Object key, Object value, int bucketIndex) {
        table[bucketIndex] = makeEntry(hash, key, value, bucketIndex);
        if (size++ >= threshold) 
            resize(2 * table.length);
    }

    protected IHashEntry makeEntry(int hash, Object key, Object value, int bucketIndex) {
			return new Entry(hash, key, value, table[bucketIndex]);
    }

    void addEntry(IHashEntry e, int bucketIndex) {
    	e.setNext(table[bucketIndex]);
			table[bucketIndex] = e;
			if (size++ >= threshold) 
					resize(2 * table.length);
    }

	  void createEntry(IHashEntry e, int bucketIndex) {
			e.setNext(table[bucketIndex]);
		  table[bucketIndex] = e;
		  size++;
	  }
	  
    /**
     * Like addEntry except that this version is used when creating entries
     * as part of Map construction or "pseudo-construction" (cloning,
     * deserialization).  This version needn't worry about resizing the table.
     *
     * Subclass overrides this to alter the behavior of HashMap(Map),
     * clone, and readObject.
     */
    void createEntry(int hash, Object key, Object value, int bucketIndex) {
        table[bucketIndex] = makeEntry(hash, key, value, bucketIndex);
        size++;
    }

    private abstract class HashIterator extends AbstractIterator {
        IHashEntry next;                  // next entry to return
        int expectedModCount;        // For fast-fail 
        int index;                   // current slot 
        IHashEntry current;               // current entry

        HashIterator() {
            expectedModCount = modCount;
            IHashEntry[] t = table;
            int i = t.length;
            IHashEntry n = null;
            if (size != 0) { // advance to first entry
                while (i > 0 && (n = t[--i]) == null) { /*loop*/ }
            }
            next = n;
            index = i;
        }

        public boolean hasNext() {
            return next != null;
        }

        IHashEntry nextEntry() { 
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            IHashEntry e = next;
            if (e == null) 
                throw new NoSuchElementException();
                
            IHashEntry n = e.getNext();
            IHashEntry[] t = table;
            int i = index;
            while (n == null && i > 0)
                n = t[--i];
            index = i;
            next = n;
            return current = e;
        }

        public void remove() {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Object k = current.getKey();
            current = null;
					CopiedHashMap2.this.removeEntryForKey(k);
            expectedModCount = modCount;
        }

    }

    private class ValueIterator extends HashIterator {
        public Object next() {
            return nextEntry().getValue();
        }
    }

    private class KeyIterator extends HashIterator {
        public Object next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator {
        public Object next() {
            return nextEntry();
        }
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    Iterator newKeyIterator()   {
        return new KeyIterator();
    }
    Iterator newValueIterator()   {
        return new ValueIterator();
    }
    Iterator newEntryIterator()   {
        return new EntryIterator();
    }


    // Views

    private transient Set entrySet = null;

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    @Starts("nothing")
	@Override
    public Set keySet() {
        Set ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private class KeySet extends AbstractSet {
        @Starts("nothing")
		@Override
        public Iterator iterator() {
            return newKeyIterator();
        }
        @Starts("nothing")
		@Override
        public int size() {
            return size;
        }
        @Starts("nothing")
		@Override
        public boolean contains(Object o) {
            return containsKey(o);
        }
        @Starts("nothing")
		@Override
        public boolean remove(Object o) {
            return CopiedHashMap2.this.removeEntryForKey(o) != null;
        }
        @Starts("nothing")
		@Override
        public void clear() {
					CopiedHashMap2.this.clear();
        }
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    @Starts("nothing")
	@Override
    public Collection values() {
        Collection vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    private class Values extends AbstractCollection {
        @Starts("nothing")
		@Override
        public Iterator iterator() {
            return newValueIterator();
        }
        @Starts("nothing")
		@Override
        public int size() {
            return size;
        }
        @Starts("nothing")
		@Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
        @Starts("nothing")
		@Override
        public void clear() {
            CopiedHashMap2.this.clear();
        }
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see Map.Entry
     */
    @Starts("nothing")
	@Override
    public Set entrySet() {
        Set es = entrySet;
        return (es != null ? es : (entrySet = new EntrySet()));
    }

    private class EntrySet extends AbstractSet {
        @Starts("nothing")
		@Override
        public Iterator iterator() {
            return newEntryIterator();
        }
        @Starts("nothing")
		@Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            IHashEntry candidate = getEntry0(e.getKey());
            return candidate != null && candidate.equals(e);
        }
        @Starts("nothing")
		@Override
        public boolean remove(Object o) {
            return removeMapping(o) != null;
        }
        @Starts("nothing")
		@Override
        public int size() {
            return size;
        }
        @Starts("nothing")
		@Override
        public void clear() {
            CopiedHashMap2.this.clear();
        }
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *		   bucket array) is emitted (int), followed  by the
     *		   <i>size</i> of the HashMap (the number of key-value
     *		   mappings), followed by the key (Object) and value (Object)
     *		   for each key-value mapping represented by the HashMap
     *             The key-value mappings are emitted in the order that they
     *             are returned by <tt>entrySet().iterator()</tt>.
     * 
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException
    {
	// Write out the threshold, loadfactor, and any hidden stuff
	s.defaultWriteObject();

	// Write out number of buckets
	s.writeInt(table.length);

	// Write out size (number of Mappings)
	s.writeInt(size);

        // Write out keys and values (alternating)
        for (Iterator i = entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    private static final long serialVersionUID = 362498820763181265L;

    /**
     * Reconstitute the <tt>HashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
	// Read in the threshold, loadfactor, and any hidden stuff
	s.defaultReadObject();

	// Read in number of buckets and allocate the bucket array;
	int numBuckets = s.readInt();
	table = new IHashEntry[numBuckets];

        init();  // Give subclass a chance to do its thing.

	// Read in size (number of Mappings)
	int size = s.readInt();

	// Read the keys and values, and put the mappings in the HashMap
	for (int i=0; i<size; i++) {
	    Object key = s.readObject();
	    Object value = s.readObject();
	    putForCreate(key, value);
	}
    }

    // These methods are used when serializing HashSets
    int   capacity()     { return table.length; }
    float loadFactor()   { return loadFactor;   }
}
