// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/StringSet.java,v 1.9 2007/01/12 18:53:28 chance Exp $
package edu.cmu.cs.fluid.util;

import java.util.*;
import com.surelogic.Starts;
import com.surelogic.common.util.AbstractRemovelessIterator;

/**
 * A Set that is optimized for, and therefore can only contain,
 * <tt>String</tt>s.  The set is designed to be reasonably
 * fast for query operations (it uses {@link java.lang.String#intern}ed
 * strings).  It will exhibit rather poor performance if elements are
 * frequently added or removed.  Null elements are not allowed.
 *
 * @author Aaron Greenhouse
 */
public class StringSet implements Set<String> {
  /** Default initial capacity. */
  private static final int DEFAULT_CAP = 10;

  /** Growth increment. */
  private static final int INCREMENT = 5;

  /**
   * The contents of the set.
   * The size of the array may grow, but never shrinks.
   * <em>The size of the set is determined by reading
   * the {@link #size} field</em>.
   */
  private String[] set;

  /** The size of the set. */
  private int size;

  //=============================================================
  //== Constructors
  //=============================================================

  /**
   * Create a new set.
   */
  public StringSet() {
    this(DEFAULT_CAP);
  }

  /**
   * Create a new set with a backing-store of the given capacity.
   */
  public StringSet(final int capacity) {
    set = new String[capacity];
    size = 0;
  }

  /**
   * Create a new set initialized to contain the strings
   * in the given array.  Any duplicate strings in the array
   * will be ignored. 
   * @exception NullPointerException Thrown if <tt>init</tt>
   * contains a <tt>null</tt> element.
   */
  public StringSet(final String[] init) {
    set = new String[init.length];
    int len = 0;

    outer : for (int i = 0; i < init.length; i++) {
      final String s = init[i].intern();
      for (int j = 0; j < len; j++) {
        if (s == set[j])
          continue outer;
      }
      set[len] = s;
      len += 1;
    }
    size = len;
  }

  /**
   * Create a new set initialized by the contents of another
   * <tt>StringSet</tt>.
   */
  public StringSet(final StringSet ss) {
    set = new String[ss.size];
    size = ss.size;
    System.arraycopy(ss.set, 0, set, 0, size);
  }

  //=============================================================
  //== Methods from Object
  //=============================================================

  @Starts("nothing")
@Override
  public boolean equals(final Object o) {
    if (o instanceof Set) {
      final Set s = (Set) o;
      if (s.size() == size) {
        for (int i = 0; i < size; i++) {
          if (!s.contains(set[i]))
            return false;
        }
        return true;
      }
    }
    return false;
  }

  @Starts("nothing")
@Override
  public int hashCode() {
    int hc = 0;
    for (int i = 0; i < size; i++) {
      hc += set[i].hashCode();
    }
    return hc;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < size; i++) {
      sb.append('\"').append(set[i]).append('\"');
      if (i != (size - 1))
        sb.append(", ");
    }
    sb.append("}");
    return sb.toString();
  }

  //=============================================================
  //== Size
  //=============================================================

  @Starts("nothing")
public int size() {
    return size;
  }

  @Starts("nothing")
public boolean isEmpty() {
    return (size == 0);
  }

  //=============================================================
  //== Membership
  //=============================================================

  @Starts("nothing")
public boolean contains(final Object o) {
    if ((o == null) || !(o instanceof String)) {
      return false;
    }

    final String s = ((String) o).intern();
    return containsInternedString(s);
  }

  /**
   * Test if an {@link java.lang.String#intern}ed String belongs 
   * to the set; use with care: 
   * <em>The caller is responisble for meeting these preconditions</em>.
   * This method has been publically exposed to allow for more
   * efficient membership testings of Strings.
   * @return <code>true</code> if the set contains the element.
   */
  public boolean containsInternedString(final String s) {
    for (int i = 0; i < size; i++) {
      if (set[i] == s)
        return true;
    }
    return false;
  }

  public boolean containsAll(final Collection c) {
    final Iterator items = c.iterator();
    while (items.hasNext()) {
      final Object o = items.next();
      if (!contains(o))
        return false;
    }
    return true;
  }

  //=============================================================
  //== Array conversion
  //=============================================================

  @Starts("nothing")
public Object[] toArray() {
    final String[] copy = new String[size];
    System.arraycopy(set, 0, copy, 0, size);
    return copy;
  }

  @SuppressWarnings("unchecked")
  public String[] toArray(Object orig[]) {
    String[] a;
    if (orig.length < size) {
      a =
        (String[]) java.lang.reflect.Array.newInstance(
          orig.getClass().getComponentType(),
          size);
    } else {
      a = (String[]) orig;
    }

    for (int i = 0; i < size; i++)
      a[i] = set[i];
    if (a.length > size)
      a[size] = null;
    return a;
  }

  //=============================================================
  //== Iterator conversion
  //=============================================================

  @Starts("nothing")
public Iterator<String> iterator() {
    return new StringSetIterator(set, size);
  }

  private static class StringSetIterator extends AbstractRemovelessIterator<String> {
    private final String[] strings;
    private int next;

    public StringSetIterator(final String[] set, final int size) {
      strings = new String[size];
      System.arraycopy(set, 0, strings, 0, size);
      next = 0;
    }

    public boolean hasNext() {
      return (next < strings.length);
    }

    public String next() {
      if (next >= strings.length) {
        throw new NoSuchElementException();
      } else {
        final String s = strings[next];
        next += 1;
        return s;
      }
    }
  }

  //=============================================================
  //== Element Addition
  //=============================================================

  public boolean add(final String o) {
    if (o == null) {
      throw new ClassCastException("Cannot add a null element.");
    }
    /*
    else if (!(o instanceof String)) {
      throw new ClassCastException("Cannot add non-String elements.");
    }
    */

    final String s = o.intern();
    return addInternedString(s);
  }

  /**
   * Add a non-<code>null</code>, {@link java.lang.String#intern}ed 
   * String to the set; use with care: 
   * <em>The caller is responisble for meeting these preconditions</em>.
   * This method has been publically exposed to allow for more
   * efficient addition of Strings to the set.
   * @return <code>true</code> if this set did not already contain
   *         the specified element.
   */
  public boolean addInternedString(final String s) {
    // Check to see if the element is already in the set.
    for (int i = 0; i < size; i++) {
      if (set[i] == s)
        return false;
    }

    if ((size + 1) > set.length) {
      final String[] newset = new String[size + INCREMENT];
      System.arraycopy(set, 0, newset, 0, size);
      set = newset;
    }
    set[size] = s;
    size += 1;

    return true;
  }

  /**
   * Unsupported operation.
   */
  public boolean addAll(final Collection c) {
    throw new UnsupportedOperationException();
  }

  //=============================================================
  //== Element Removal
  //=============================================================

  @Starts("nothing")
public void clear() {
    size = 0;
  }

  @Starts("nothing")
public boolean remove(final Object o) {
    if ((o == null) || !(o instanceof String)) {
      return false;
    }

    final String s = ((String) o).intern();
    return removeInternedString(s);
  }

  /**
   * Remove an {@link java.lang.String#intern}ed String from the set;
   * use with care: 
   * <em>The caller is responisble for meeting these preconditions</em>.
   * This method has been publically exposed to allow for more
   * efficient removal of Strings from the set.
   * @return <code>true</code> if the set contained the element.
   */
  public boolean removeInternedString(final String s) {
    int loc = -1;
    for (int i = 0;(i < size) && (loc == -1); i++) {
      if (set[i] == s)
        loc = i;
    }

    if (loc != -1) {
      final int src = loc + 1;
      System.arraycopy(set, src, set, loc, size - src);
      size -= 1;
      return true;
    }
    return false;
  }

  /**
   * Unsupported Operation.
   */
  public boolean removeAll(final Collection c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported Operation.
   */
  public boolean retainAll(final Collection c) {
    throw new UnsupportedOperationException();
  }

  //=============================================================
  //== Test Program
  //=============================================================

  public static void main(String[] args) {
    StringSet set = new StringSet(args);
    System.out.println(set);

    System.out.println("\nAdding q");
    set.add("q");
    System.out.println(set);

    System.out.println("\nAdding l");
    set.addInternedString("l");
    System.out.println(set);

    System.out.println("\nTesting for q: " + set.containsInternedString("q"));
    System.out.println("Testing for l: " + set.contains("l"));

    System.out.println("\nRemoving q");
    set.remove("q");
    System.out.println(set);

    System.out.println("\nRemoving l");
    set.remove("l");
    System.out.println(set);

    System.out.println("\nTrying iterator:");
    final Iterator iter = set.iterator();
    while (iter.hasNext()) {
      System.out.println(iter.next());
    }
  }
}
