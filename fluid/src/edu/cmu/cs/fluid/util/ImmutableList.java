/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/ImmutableList.java,v 1.9 2007/04/03 10:44:15 boyland Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.AbstractSequentialList;
import java.util.ListIterator;
import java.util.List;
import java.util.NoSuchElementException;
import com.surelogic.Starts;

/** An implementation of lists that are immutable.
 * Traversing the list backwards is very inefficient.
 * The lists are implemented using immutable cons-cells.
 * <p>We may wish to rename this class to <tt>ImmutableLinkedList</tt>
 * and have this name be used by an interface as in {@link ImmutableSet}.
 */
public class ImmutableList<T extends Object> extends AbstractSequentialList<T>
{
  /*
   * An empty list (that can be safely cast to any type).
   */
  public static final ImmutableList<Object> nil = new ImmutableList<Object>((ConsCell<Object>)null);

  public static <T extends Object> ImmutableList<T> nil() {
    return new ImmutableList<T>((ConsCell<T>)null);
  }
  
  /**
   * Create a one element immutable list.
   * @param item
   */
  public ImmutableList(T item) {
    this(new ConsCell<T>(item,null));
  }
  
  /**
   * Create a list by adding an element to the front of an existing list.
   */
  public ImmutableList(T item, ImmutableList<T> rest) {
    this(new ConsCell<T>(item,rest.head));
  }

  /**
   * Create a list by adding an element to the front of an existing list.
   */
  public static <T extends Object> ImmutableList<T> cons(T item, ImmutableList<T> rest) {
    return new ImmutableList<T>(item,rest);
  }

  public T head() {
    if (isEmpty()) throw new NoSuchElementException("head of empty list");
    return head.car;
  }
  
  public ImmutableList<T> tail() {
    if (isEmpty()) return this;
    return new ImmutableList<T>(head.cdr);
  }
  
  /**
   * Create an Immutable List from an array
   */
  public ImmutableList(T[] array) {
    this(array,0,array.length);
  }

  /**
   * Create an Immutable List from an array
   */
  public ImmutableList(T[] array, int from, int size) {
    if (from < 0 || size < 0 || from+size > array.length) {
      throw new IndexOutOfBoundsException("ImmutableList called with bad bounds");
    }
    ConsCell<T> t = null;
    while (size-- > 0) {
      t = new ConsCell<T>(array[from+size],t);
    }
    head = t;
  }


  /// Basic structure

  private static class ConsCell<T1> {
    public final T1 car;
    public final ConsCell<T1> cdr;
    public final int length;
    public ConsCell(T1 a, ConsCell<T1> d) {
      car = a; cdr = d;
      length = (d == null) ? 1 : d.length+1;
    }
  }

  private final ConsCell<T> head;
  
  private ImmutableList(ConsCell<T> h) {
    head = h;
  }

  /// List implementation

  @Starts("nothing")
@Override
  public boolean isEmpty() {
    return head == null;
  }

  @Starts("nothing")
@Override
  public int size() {
    return (head == null) ? 0 : head.length;
  }

  private static boolean equals(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  // the inherited implementation calls the very slow operation
  // previous, which would make this method O(n^2)
  @Starts("nothing")
@Override
  public int lastIndexOf(Object o) {
    int i=0;
    int last = -1;
    for (ConsCell p = head; p != null; p = p.cdr, ++i) {
      if (equals(o,p.car)) last = i;
    }
    return last;
  }

  /**
   * The iterator for the immutable list class.
   * Backward traversal is very slow, but
   * added to meet the requirements of the List class.  A
   * {@link #previous} call takes O(n) time.
   */
  private class Iterator extends AbstractListIterator<T> {
    ConsCell<T> p;
    int index = 0;
    public Iterator (ConsCell<T> c) { p = c; }
    public void add(T o) {
      throw new UnsupportedOperationException("ImmutableList.add");
    }
    public boolean hasNext() {
      return p != null;
    }
    public boolean hasPrevious() {
      return index > 0;
    }
    public T next() {
      if (p == null) throw new NoSuchElementException("end of ImmutableList");
      T r = p.car;
      p = p.cdr;
      ++index;
      return r;
    }
    public int nextIndex() {
      return index;
    }
    public T previous() {
      //! slow function
      if (index == 0) throw new NoSuchElementException("start of ImmutableList");
      p = ((Iterator)listIterator(index-1)).p;
      --index;
      return p.car;
    }
    public int previousIndex() {
      return index-1;
    }
    public void remove() {
      throw new UnsupportedOperationException("ImmutableList.remove");
    }
    public void set(T o) {
      throw new UnsupportedOperationException("ImmutableList.set");
    }
  }

  @Starts("nothing")
@Override
  public ListIterator<T> listIterator(int x) {
    if (x < 0 || x > size()) {
      throw new IndexOutOfBoundsException("listIterator");
    }
    Iterator it = new Iterator(head);
    while (x > 0) {
      it.next();
      --x;
    }
    return it;
  }
}

class TestImmutableList {
  static boolean verbose = false;

  public void reportError(String msg) {
    System.out.println("\n!!! "+msg);
  }
  
  public static void main(String[] args) {
    new TestImmutableList().test(args);
  }
  
  void test(String[] args) {   
    if (new ImmutableList<String>(args).contains("-v")) {
      verbose = true;
    }
    doTest(new Object[0],-1,-1);
    doTest(args,-1,-1);
    int[] testvals = {1, 8, 2, -5, 7, 000, 9, 1, -2, 4, 3, 2, 5, 7};
    Integer[] array = new Integer[testvals.length];
    for (int i=0; i < testvals.length; ++i) {
      array[i] = new Integer(testvals[i]);
    }
    doTest(array,-1,2);
    array[5] = null;
    doTest(array,5,2);
  }

  private <T extends Object> void doTest(T[] array, int nullIndex, int dupIndex) {
    if (verbose) {
      System.out.print("Testing with [");
      for (int i=0; i < array.length; ++i) {
	if (i > 0) System.out.print(",");
	System.out.print(array[i]);
      }
      System.out.println("]");
    }
    List<T> l1 = makeList(array,ImmutableList.<T>nil());
    List<T> l2 = new ImmutableList<T>(array);
    List<Object> l3 = makeList(array,ImmutableList.cons("last",ImmutableList.nil()));

    if (l1.hashCode() != l2.hashCode()) {
      reportError("hashCode not consistent\n");
    }
    if (l1.hashCode() == l3.hashCode()) {
      reportError("hashCode doesn't seem to be working well\n");
    }
    if (l1.size() != array.length || l2.size()+1 != l3.size()) {
      reportError("size() not working\n");
    }
    try {
      l3.clear();
      reportError("clear should not be implemented\n");
    } catch (UnsupportedOperationException ex) {
      // expected
    }
    if (l1.isEmpty() != (array.length == 0)) {
      reportError("isEmpty() not working\n");
    }
    Object[] arr = l1.toArray();
    for (int i=0; i < array.length; ++i) {
      if (arr[i] != array[i]) {
	reportError("toArray not working\n");
      }
    }
    for (int i=0; i < array.length; ++i) {
      if (l2.get(i) != array[i]) {
	reportError("get not working\n");
      }
    }
    try {
      l3.remove(0);
      reportError("remove(int) should not be implemented\n");
    }  catch (UnsupportedOperationException ex) {
      // expected
    }
    try {
      if (array.length > 0) {
        l1.add(array[0]);
        reportError("add should not be implemented\n");
      }
    }  catch (UnsupportedOperationException ex) {
      // expected
    }
    if (l1.indexOf(null) != nullIndex ||
	l3.indexOf(null) != nullIndex) {
      reportError("indexOf(null) not working\n");
    }
    if (l1.lastIndexOf(null) != nullIndex ||
	l3.lastIndexOf(null) != nullIndex) {
      reportError("lastIndexOf(null) not working\n");
    }
    Object unique = new Object();
    if (l1.indexOf(unique) != -1 || l2.lastIndexOf(unique) != -1) {
      reportError("indexOf(unique) or lastIndexOf(unique) not working\n");
    }
    for (int i=0; i < array.length; ++i) {
      int index1 = l3.indexOf(array[i]);
      int index2 = l3.lastIndexOf(array[i]);
      if (index1 < 0 || index2 < 0 || index1 > index2) {
	reportError("indexOf/lastIndexOf problems\n");
      }
      if (index1 == dupIndex && index2 == dupIndex) {
	reportError("lastIndex not working\n");
      }
    }
    try {
      l3.remove("last");
      reportError("remove(Object) should not be implemented\n");
    }  catch (UnsupportedOperationException ex) {
      // expected
    }
    try {
      l3.set(0,unique);
      reportError("set should not be implemented\n");
    }  catch (UnsupportedOperationException ex) {
      // expected
    }
  }

  private static <T extends Object> ImmutableList<T> makeList(T[] elems, ImmutableList<T> l) {
    for (int i=elems.length; i > 0; --i) {
      l = ImmutableList.cons(elems[i-1],l);
    }
    return l;
  }
}
