/*$Header$*/
package edu.cmu.cs.fluid.util;

import org.apache.commons.collections15.IterableMap;
import com.surelogic.Starts;

/**
 * The result of refactoring out the commonality in Customizable{Hashed,HashCode}Map
 * 
 * @author chance
 */
public abstract class AbstractHashMap<K,V> implements IterableMap<K,V> {
  protected static final String NO_NEXT_ENTRY = "No next() entry in the iteration";
  protected static final String NO_PREVIOUS_ENTRY = "No previous() entry in the iteration";
  protected static final String REMOVE_INVALID = "remove() can only be called once after next()";
  protected static final String GETKEY_INVALID = "getKey() can only be called after next() and before remove()";
  protected static final String GETVALUE_INVALID = "getValue() can only be called after next() and before remove()";
  protected static final String SETVALUE_INVALID = "setValue() can only be called after next() and before remove()";

  /**
   * The default capacity to use
   */
  protected static final int DEFAULT_CAPACITY = 16;
  /**
   * The default threshold to use
   */
  protected static final int DEFAULT_THRESHOLD = 12;
  /**
   * The default load factor to use
   */
  protected static final float DEFAULT_LOAD_FACTOR = 0.75f;
  /**
   * The maximum capacity allowed
   */
  protected static final int MAXIMUM_CAPACITY = 1 << 30;
  /**
   * An object for masking null
   */
  protected static final Object NULL = new Object();
  protected static final int NULL_HASH = NULL.hashCode();  
  
  /**
   * Load factor, normally 0.75
   */
  protected transient float loadFactor;
  /**
   * The size of the map
   */
  protected transient int size;
  /**
   * Size at which to rehash
   */
  protected transient int threshold;
  /**
   * Modification count for iterators
   */
  protected transient int modCount;
  
  //-----------------------------------------------------------------------
  
  /**
   * Initialise subclasses during construction, cloning or deserialization.
   */
  protected void init() {
  }

  //-----------------------------------------------------------------------
  
  /**
   * Gets the size of the map.
   *
   * @return the size
   */
  @Starts("nothing")
public int size() {
      return size;
  }
  
  /**
   * Checks whether the map is currently empty.
   *
   * @return true if the map is currently size zero
   */
  @Starts("nothing")
public boolean isEmpty() {
      return (size == 0);
  }
  
  //-----------------------------------------------------------------------
  
  /**
   * Compares two keys, in internal converted form, to see if they are equal.
   * This implementation uses the equals method.
   * Subclasses can override this to match differently.
   *
   * @param key1 the first key to compare passed in from outside
   * @param key2 the second key extracted from the entry via <code>entry.key</code>
   * @return true if equal
   */
  protected boolean isEqualKey(Object key1, Object key2) {
      return (key1 == key2 || ((key1 != null) && key1.equals(key2)));
  }

  /**
   * Compares two values, in external form, to see if they are equal.
   * This implementation uses the equals method and assumes neither value is null.
   * Subclasses can override this to match differently.
   *
   * @param value1 the first value to compare passed in from outside
   * @param value2 the second value extracted from the entry via <code>getValue()</code>
   * @return true if equal
   */
  protected boolean isEqualValue(Object value1, Object value2) {
      return (value1 == value2 || value1.equals(value2));
  }
  
  /**
   * Gets the index into the data storage for the hashCode specified.
   * This implementation uses the least significant bits of the hashCode.
   * Subclasses can override this to return alternate bucketing.
   *
   * @param hashCode the hash code to use
   * @param dataSize the size of the data to pick a bucket from
   * @return the bucket index
   */
  protected int hashIndex(int hashCode, int dataSize) {
      return hashCode & (dataSize - 1);
  }
  
  //-----------------------------------------------------------------------
  
  /**
   * Calculates the new capacity of the map.
   * This implementation normalizes the capacity to a power of two.
   *
   * @param proposedCapacity the proposed capacity
   * @return the normalized new capacity
   */
  protected int calculateNewCapacity(int proposedCapacity) {
      int newCapacity = 1;
      if (proposedCapacity > MAXIMUM_CAPACITY) {
          newCapacity = MAXIMUM_CAPACITY;
      } else {
          while (newCapacity < proposedCapacity) {
              newCapacity <<= 1;  // multiply by two
          }
          if (newCapacity > MAXIMUM_CAPACITY) {
              newCapacity = MAXIMUM_CAPACITY;
          }
      }
      return newCapacity;
  }

  /**
   * Calculates the new threshold of the map, where it will be resized.
   * This implementation uses the load factor.
   *
   * @param newCapacity the new capacity
   * @param factor      the load factor
   * @return the new resize threshold
   */
  protected int calculateThreshold(int newCapacity, float factor) {
      return (int) (newCapacity * factor);
  }
}
