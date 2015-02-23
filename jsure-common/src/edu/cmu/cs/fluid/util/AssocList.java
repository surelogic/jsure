package edu.cmu.cs.fluid.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import com.surelogic.Starts;

/** Association list dictionary:
 * Low space overhead but O(n) search time.
 * @typeparam Key type of keys in the list
 * <dl purpose=fluid>
 *   <dt>capabilities<dd> store, read, equal
 * </dl>
 * @typeparam Value
 */
public class AssocList<K,V> extends Dictionary<K,V> {
  /** The key here (or null in an empty alist)
   * @type Key
   */
  protected K key;
  /** The value associated with this key
   * @type Value
   * @invariant private (nonNull(key)||Null(value))&&
   *                    (Null(key)||NonNull(value))
   */
  protected V value;

  /** The other entries.
   * @invariant private nonNull(key)||Null(rest)
   * @structure
   */
  protected AssocList<K,V> rest;

  /** Create an empty association list */
  public AssocList() { }

  /** Create an association list with a single entry
   * @param key initial key
   * <dl purpose=fluid>
   *   <dt>type<dd> Key
   * </dl>
   * @param value initial value
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   * @precondition nonNull(key)
   */
  public AssocList(K key, V value) {
    if (value != null) {
      this.key = key;
      this.value = value;
    }
  }

  /** return number of distinct entries
   * @pure
   */
  @Starts("nothing")
@Override
  public final int size() {
  	/*
    int here = 0;
    if (key != null) ++here;
    if (rest == null) return here;
    return here + rest.size();
    */
    return loopSize(this);
  }
  
  @Starts("nothing")
  private static int loopSize(AssocList cursor) {
		int here = 0;
		while (cursor != null) {
  		if (cursor.key != null) ++here;
  		cursor = cursor.rest;
		}
		return here;
  }

  /** return whether no distinct entries
   * @pure
   */
  @Starts("nothing")
@Override
  public boolean isEmpty() {
    return key == null && (rest == null || rest.isEmpty());
  }

  /** Look up the value for this key
   * @pure
   * @precondition nonNull(key)
   * @param key key to use
   * <dl purpose=fluid>
   *   <dt>type<dd> Key
   * </dl>
   * @return the value associated with the key (or null)
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   */
  @Starts("nothing")
@Override
  public final V get(Object key) {
    /*
    if (this.key != null && key.equals(this.key)) return value;
    if (rest == null) return null;
    return rest.get(key);
    */
    return loopGet(this, key);
  }
  
  @Starts("nothing")
  private static <K,V> V loopGet(AssocList<K,V> cursor, final Object key) {
    //calls++;
    while (cursor != null) {
      //total++;
    	
      if (cursor.key != null && key.equals(cursor.key)) return cursor.value;
      cursor = cursor.rest;
    }
    return null;
  }
 
  static long calls = 0;
  static long total = 0;
  
  protected final V get2(Object key) {
    /*
    if (this.key != null && key.equals(this.key)) return value;
    if (rest == null) return null;
    return rest.get(key);
    */
    return loopGet2(this, key);
  }
  
  protected static <K,V> V loopGet2(AssocList<K,V> cursor, final Object key) {
    calls2++;
    while (cursor != null) {
      total2++;
      
      if (cursor.key != null && key.equals(cursor.key)) return cursor.value;
      cursor = cursor.rest;
    }
    return null;
  }
 
  static long calls2 = 0;
  static long total2 = 0;
  
  public static void printTotal() {
    System.out.println("Calls to loopGet() = "+calls);
    System.out.println("Avg iterations = "+total / (double) calls);
    
    System.out.println("Calls to loopGet2() = "+calls2);
    System.out.println("Avg iterations = "+total2 / (double) calls2);
  }
  
  /** Change the value for this key
   * @precondition nonNull(key) && nonNull(newValue)
   * @param key key to use
   * <dl purpose=fluid>
   *   <dt>type<dd> Key
   * </dl>
   * @param newValue value to associate with the key
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   * @return the value formerly associated with the key (or null)
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   */
  @Override
  public V put(K key, V newValue) {
  	/*
    if (this.key == null) {
      this.key = key;
      this.value = newValue;
      return null;
    } else if (key.equals(this.key)) {
      Object oldValue = this.value;
      this.value = newValue;
      return oldValue;
    } else if (rest == null) {
      rest = new AssocList(key,newValue);
      return null;
    } else {
      return rest.put(key,newValue);
    }
    */
		if (this.key == null) {
			this.key = key;
			this.value = newValue;
			return null;
		} 
    return loopPut(this, key, newValue);
  }

  private static <K,V> V loopPut(AssocList<K,V> cursor, final K key, final V newValue) {
  	// cursor assumed !null the first time
  	while (true) {
			if (key.equals(cursor.key)) {
				V oldValue = cursor.value;
				cursor.value = newValue;
				return oldValue;
			} else if (cursor.rest == null) {
				cursor.rest = new AssocList<K,V>(key,newValue);
				return null;
			}			
			cursor = cursor.rest;
  	}
  }

  /** Remove the value for this key
   * @precondition nonNull(key)
   * @param key key to use
   * <dl purpose=fluid>
   *   <dt>type<dd> Key
   * </dl>
   * @return the value formerly associated with the key (or null)
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   */
  @Starts("nothing")
@Override
  public V remove(Object key) {
    if (this.key == null) {
      return null;
    } else if (key.equals(this.key)) {
      V oldValue = this.value;
      if (rest == null) {
      	this.key = null;
	      this.value = null;
      } else {
	      this.key = rest.key;
	      this.value = rest.value;
	      this.rest = rest.rest;
      }
      return oldValue;
    } else if (rest == null) {
      return null;
    } else {
      //return rest.remove(key);
			return loopRemove(this, key);      
    }
  }
  
  @Starts("nothing")
  private static <K,V> V loopRemove(AssocList<K,V> cursor, final Object key) {
  	while (cursor != null) {
			AssocList<K,V> rest = cursor.rest;
		  if (cursor.key == null) {
			  return null;
		  } else if (key.equals(cursor.key)) {
  			V oldValue = cursor.value;
			  if (rest == null) {
					cursor.key = null;
				  cursor.value = null;
			  } else {
				  cursor.key = rest.key;
					cursor.value = rest.value;
					cursor.rest = rest.rest;
			  }
			  return oldValue;
		  } 
		  cursor = rest;
    }
  	return null;
  }

  /** Return the keys for which we have entries.
   * @pure
   * @return an enumeration of keys
   * <dl purpose=fluid>
   *   <dt>type<dd> Enumeration[Key]
   *   <dt>capabilities<dd> cast, read, write
   * </dl>
   */
  @Starts("nothing")
@Override
  public Enumeration<K> keys() {
    return new AssocKeyEnumeration<K>(this);
  }

  /** Return the values in all entries.
   * @pure
   * @return an enumeration of the elements (values associated with keys)
   * <dl purpose=fluid>
   *   <dt>type<dd> Enumeration[Value]
   *   <dt>capabilities<dd> cast, read, write
   * </dl>
   */
  @Starts("nothing")
@Override
  public Enumeration<V> elements() {
    return new AssocValueEnumeration<V>(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (AssocList l = this; l != null; l=l.rest) {
      sb.append("(");
      sb.append(l.key);
      sb.append(":");
      sb.append(l.value);
      sb.append(")");
      if (l.rest != null) sb.append(" ");
    }
    sb.append("}");
    return sb.toString();
  }
}

class AssocKeyEnumeration<K> implements Enumeration<K> {
  AssocList<K,?> cursor;
   
  @Starts("nothing")
  AssocKeyEnumeration(AssocList<K,?> start) {
    cursor = start;
  }

  /** Return whether the enumeration is nonempty.
   * @pure
   * @read cursor
   */
  public boolean hasMoreElements() {
    return cursor != null && !cursor.isEmpty();
  }

  /** Return the next element in the enumeration.
   * @exception NoSuchElementException
   * If the enumeration is empty.
   * <dl purpose=fluid>
   *  <dt>condition<dd> equal(hasMoreElements(),false)
   * </dl>
   * @read cursor
   * @write cursor
   */
  public K nextElement() {
    if (cursor == null || cursor.key == null) {
      throw new NoSuchElementException("end of association list");
    } else {
      K key  = cursor.key;
      cursor = cursor.rest;
      return key;
    }
  }
}

class AssocValueEnumeration<V> implements Enumeration<V> {
  AssocList<?,V> cursor;
  
  @Starts("nothing")
  AssocValueEnumeration(AssocList<?,V> start) {
    cursor = start;
  }

  /** Return whether the enumeration is nonempty.
   * @pure
   * @read cursor
   */
  public boolean hasMoreElements() {
    return cursor != null && !cursor.isEmpty();
  }

  /** Return the next element in the enumeration.
   * @exception NoSuchElementException
   * If the enumeration is empty.
   * <dl purpose=fluid>
   *  <dt>condition<dd> equal(hasMoreElements(),false)
   * </dl>
   * @read cursor
   * @write cursor
   */
  public V nextElement() {
    if (cursor == null || cursor.key == null) {
      throw new NoSuchElementException("end of association list");
    } else {
      V value=cursor.value;
      cursor = cursor.rest;
      return value;
    }
  }
}

