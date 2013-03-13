/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/ImmutableHashOrderSet.java,v 1.42 2008/12/12 19:01:02 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.AbstractRemovelessIterator;
import com.surelogic.Starts;

/** An implementation of sets with some provision for infinite sets
 * whose inverse is finite.  The sets are very general: they may
 * contain any objects, but those with useful hash codes are
 * handled more efficiently.
 * <p> The sets is maintained as an "almost sorted" array of elements
 * enabling set inclusion to be tested in O(lg n) time.  "Almost
 * sorted" because the hash code is used to sort the elements
 * and elements with the same hash code are unordered. </p>
 */
//@SuppressWarnings("all")
public class ImmutableHashOrderSet<T> implements ImmutableSet<T>
{
  public static final boolean debug = false; //XUtil.useExperimental;

  /** This array holds exactly the elements in the set (or not in
   * the set if inverse is true.  They are sorted by hashCode.
   */
  protected final T[] elements;

  /** This boolean value is true for infinite sets with finite
   * inverses, in which case the elements array holds the elements
   * in the inverse.
   */
  protected final boolean inverse;
  
  private final int hashCode;

  private int computeHashCode() {
    int h = SortedArray.hashCode(elements);
    if (inverse) return -h;
    else return h;
  }
  
  /**
   * Create an empty set.
   */
  @SuppressWarnings("unchecked")
  public ImmutableHashOrderSet()
  {
    this(false, (T[]) SortedArray.empty());
  }

  /**
   * Create a set using the elements of an existing
   * <code>Collection</code>.
   * @param c The collection to use.
   */
  @SuppressWarnings("unchecked")
  public ImmutableHashOrderSet( final Collection<? extends T> c )
  {
    this((T[]) c.toArray(),false);
  }

  /**
   * Create a set with the given elements
   * The array must be unique.
   */
  public ImmutableHashOrderSet(T[] elems) {
    //@ unique(elems);
    this(elems,false);
  }

  /**
   * Create a set.
   * @param elems a unique array of elements to be in (not in)
   * the set.
   * @param inv true if we want the infinite set which has every
   * element not in the array.
   */
  public ImmutableHashOrderSet(T[] elems, boolean inv) {
    this(inv,SortedArray.uniq(SortedArray.sort(elems)));
  }

  /**
   * Create a set 
   * @param inv whether the set is infinite (or not)
   * @param elems Assumed to be sorted already
   */
  private ImmutableHashOrderSet(boolean inv, T[] elems) {
    elements = elems;
    inverse = inv;  	
    hashCode = computeHashCode(); 		
    if (debug) {
    	count(elems.length);
    }
  }
  
  private static final boolean useStdDev = false;  
  private static final boolean useCounts = false;
  
  static synchronized void count(final int size) {
	  num++;
	  sum += size;
	  if (size > max) {		  
		  max = size;
		  /*
		  if (size > 50) {
			  System.out.println("Too big: "+size);
		  }
		  */
	  }
	  if (useStdDev) {
		  if (useCounts) {
			  int i = counts.size();
			  if (i <= size) {
				  // Fill in blanks
				  for(; i<=size; i++) {
					  counts.add(0);
				  }
				  counts.add(1);
			  } else {
				  int current = counts.get(size);
				  counts.set(size, current+1);
			  }

			  if ((num & 0x7fffff) == 0) {
				  printStats();
			  }
		  } else {
			  // Update Ai and Qi
			  final double Aold = Ai;
			  final double Qold = Ai;
			  final double diff = size - Aold;
			  Ai = Ai + diff / num;
			  Qi = Qi + diff * (size - Ai);
		  }
	  }
  }

  // Using http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
  static double Ai = 0;
  static double Qi = 0;
  static int num = 0;
  static long sum = 0;
  static int max = 0;
  static final List<Integer> counts = new ArrayList<Integer>();
  
  static String printStats() {
	  //System.out.println("Max:     "+max);
	  double avg = sum / (double) num;
	  //System.out.println("Average: "+avg+" for "+num);		  
	  
	  if (useStdDev) {
		  // Compute standard deviation
		  double deviation = 0;
		  if (useCounts) {
			  int i=0;
			  for(Integer iv : counts) {
				  if (iv != 0) {
					  double diff = avg - i;			  
					  deviation += iv*(diff*diff);		
					  //System.out.println("Count:  "+iv+" at "+i);
				  }
				  i++;			  
			  }
		  } else {
			  deviation = Qi;
		  }
		  double sd = Math.sqrt(deviation/(num-1));
		  //System.out.println("Std dev: "+sd);	  
		  return ", "+num+", "+avg+", "+sd+", "+max;
	  } else {
		  return ", "+num+", "+avg+", "+max;
	  }
  }
  
  static synchronized String clearStats() {
	  String rv = printStats();
	  
	  Ai = 0;
	  Qi = 0;
	  num = 0;
	  sum = 0;
	  max = 0;	  
	  counts.clear();
	  return rv;
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final ImmutableHashOrderSet empty = new ImmutableHashOrderSet(SortedArray.empty,false);
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static final ImmutableHashOrderSet universe = new ImmutableHashOrderSet(SortedArray.empty,true);

  @SuppressWarnings("unchecked")
  public static <V> ImmutableHashOrderSet<V> emptySet() {
    return empty;
  }
  
  @SuppressWarnings("unchecked")
  public static <V> ImmutableHashOrderSet<V> universeSet() {
    return universe;
  }
  
  /** Return the set including every element but the ones in this set.
   */
  public ImmutableHashOrderSet<T> invert() {
    return new ImmutableHashOrderSet<T>(!inverse, elements);
  }

  @Override
  public ImmutableSet<T> invertCopy() {
    return invert();
  }

  @Starts("nothing")
@Override
  public final int hashCode() {
  	return hashCode;
  }

  @Starts("nothing")
@Override
  public boolean equals(Object other) {
    if (other instanceof ImmutableHashOrderSet) {
      return equals((ImmutableHashOrderSet<?>)other);
    } else {
      // other can be null; this cannot be null
      return (other == null) ? false : other.equals(this);
//      return other.equals( this );
    }
  }
  
  public boolean equals(ImmutableHashOrderSet<?> other) {
    return equals(this,other);
  }

  public static boolean equals(ImmutableHashOrderSet<?> s1, ImmutableHashOrderSet<?> s2) {
    if (s1.inverse != s2.inverse) return false;
    if (s1.hashCode != s2.hashCode) {
    	return false;
    }
    return SortedArray.equals(s1.elements,s2.elements);
  }

  @Override
  @Starts("nothing")
public boolean isEmpty() {
    return !inverse && elements.length == 0;
  }

  @Override
  public boolean isInfinite() {
    return inverse;
  }

  /** Index into the set (which must be finite) and
   * get an element.
   */
  public Object elementAt(int i) throws SetException {
    if (isInfinite()) throw new SetException("can't index in an infinite set");
    if (i < 0 || elements.length <= i)
      throw new SetException("index out of range");
    return elements[i];
  }

  /**
   * Return the cardinality of the set.  To conform to the java.util.Set
   * contract, the size of an infinite set is <code>Integer.MAX_VALUE</code>.
   * If the return value is <code>Integer.MAX_VALUE</code>, a call
   * should be made to {@link #isInfinite} to check if the set is 
   * indeed infinite.
   * @return The number of elements in the set, or
   * <code>Integer.MAX_VALUE</code> if the set is infinite.
   */
  @Override
  @Starts("nothing")
public int size()
  {
    if( inverse )
      return Integer.MAX_VALUE;
    else
      return elements.length;
  }

  public Enumeration<T> elements() throws SetException {
    if (inverse) throw new SetException("infinite enumeration");
    return SortedArray.elements(elements);
  }

  /**
   * Returns an iterator over the elements of the set.  The
   * elements are sorted by hash code.  The iterator does not
   * support the remove operation.
   *
   * <P>Because this method is not allowed to throw an exception,
   * even if the set is infinite, it instead returns 
   * a useless iterator that churns out a series of
   * objects not in the set.  The iterator never completes.
   * @return An iterator over the elements.
   */
  @Override
  @Starts("nothing")
public Iterator<T> iterator()
  {
    if (inverse) return new AbstractRemovelessIterator<T>() {
      @Override
      public boolean hasNext() { return true; }
      @Override
      @SuppressWarnings("unchecked")
      public T next() { return (T) new Object(); }
    };
    return SortedArray.iterator( elements );
  }

  /**
   * Get an array of all the elements in the set.  The array is
   * sorted in hash code order.
   *
   * @return An array containing all the elements of the set.
   *
   * @exception OutOfMemoryError Thrown if the set is infinite.
   */
  @Override
  @Starts("nothing")
public Object[] toArray()
  {
    if (inverse) return toArray(new Object[size()]);
    return elements.clone();
  }

  /**
   * Get an array of all the elements in the set.  The array is sorted
   * in hashcode order.  If the set is infinite, we try to allocate an
   * infinite array, yielding OutOfMemoryError
   * contains the inverse of the set [huh? this makes no sense&mdash;Aaron].
   *
   * @param  a the array into which the elements of the collection are to
   * 	       be stored, if it is big enough; otherwise, a new array of the
   * 	       same runtime type is allocated for this purpose.
   * @return an array containing the elements of the collection.
   * 
   * @exception NullPointerException if the specified array is <tt>null</tt>.
   * 
   * @exception ArrayStoreException if the runtime type of the specified array
   *         is not a supertype of the runtime type of every element in this
   *         collection.
   *
   * @exception OutOfMemoryError Thrown if the set is infinite.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <V> V[] toArray( V[] a )
  {
    if( a.length < size())
      a = (V[])java.lang.reflect.Array.newInstance(
            a.getClass().getComponentType(), size() );

    if (inverse) { // this code should be unreachable
      throw new OutOfMemoryError("cannot allocate infinite array");
    }

    System.arraycopy( elements, 0, a, 0, elements.length );

    if( a.length > elements.length )
      a[elements.length] = null;
    return a;
  }

  /**
   * Test if the set contains a given element.
   * @param elem Object to test
   * @return <code>true</code> iff the set contains an element
   * <code>e</code> s.t. <code>e.equals( elem )</code>
   */
  @Override
  @Starts("nothing")
public boolean contains( final Object elem )
  {
    return inverse != SortedArray.contains(elements,elem);
  }

  /**
   * Test if the set contains all the elements of a given collection.
   * @param c The collection to test.
   * @return <code>true</code> iff all the elements of <code>c</code>
   * are contained in this set.
   */
  @Override
  @Starts("nothing")
public boolean containsAll( final Collection<?> c )
  {
    // Quick and dirty checks for some of the easy infinite cases
    
    // True if this set is the universe
    if (inverse && elements.length == 0) {
      return true;
    }

    // This set is not the universe, so result is false if the other set is the universe
    if (c instanceof ImmutableHashOrderSet) {
      final ImmutableHashOrderSet<?> other = (ImmutableHashOrderSet<?>) c;
      if (other.inverse && other.elements.length == 0) {
        return false;
      }
    }
    
    boolean flag = true;
    final Iterator<?> elts = c.iterator();
    while( flag && elts.hasNext() )
    {
      flag = contains( elts.next() );
    }
    return flag;
  }

  /**
   * Add all the elements of a collection to this set.
   * Unsupported operation.
   * @throws UnsupportedOperationException Always thrown
   */
  @Override
  public boolean addAll( final Collection<? extends T> c )
  {
    throw new UnsupportedOperationException( getClass().getName()
                                           + " does not support addAll()" );
  }

  /**
   * Keep only the elements present in the given collection.
   * Unsupported operation.
   * @throws UnsupportedOperationException Always thrown
   */
  @Override
  @Starts("nothing")
public boolean retainAll( final Collection<?> c )
  {
    throw new UnsupportedOperationException( getClass().getName()
                                           + " does not support retainAll()" );
  }

  /**
   * Remove all the elements present in a given collection.
   * Unsupported operation.
   * @throws UnsupportedOperationException Always thrown
   */
  @Override
  @Starts("nothing")
public boolean removeAll( final Collection<?> c )
  {
    throw new UnsupportedOperationException( getClass().getName()
                                           + " does not support removeAll()" );
  }

  /**
   * Remove all the elements of this set.
   * Unsupported operation.
   * @throws UnsupportedOperationException Always thrown
   */
  @Override
  @Starts("nothing")
public void clear()
  {
    throw new UnsupportedOperationException( getClass().getName()
                                           + " does not support clear()" );
  }

  public static String clearCaches() {	  
	  SortedArray.clearCaches();
	  return clearStats();
  }
  
  /** True if this set includes every element
   * in the argument set.
   */
  public boolean includes(ImmutableHashOrderSet<?> other) {
    if (inverse) {
      if (other.inverse) {
	return SortedArray.includes(other.elements,elements);
      } else {
	return !SortedArray.overlaps(elements,other.elements);
      }
    } else {
      if (other.inverse) {
	return false;
      } else {
	return SortedArray.includes(elements,other.elements);
      }
    }
  }

  /** True if this set includes some element
   * also included by the argument set.
   */
  public boolean overlaps(ImmutableHashOrderSet<?> other) {
    if (inverse) {
      if (other.inverse) {
	return true;
      } else {
	return !SortedArray.includes(elements,other.elements);
      }
    } else {
      if (other.inverse) {
	return !SortedArray.includes(other.elements,elements);
      } else {
	return SortedArray.overlaps(elements,other.elements);
      }
    }
  }

  /**
   * Add an element to the set.  Unsupported.
   * @throws UnsupportedOperationException Always thrown
   * @see #addElement(Object)
   */
  @Override
  public boolean add( final T elem )
  {
    throw new UnsupportedOperationException( getClass().getName()
                                           + " does not support add()" );
  }

  /** 
   * Return a new set that includes the given element.
   */
  @Override
  public ImmutableSet<T> addCopy(T elem) {
    return addElement(elem);
  }
  
  /** 
   * Return a new set that includes the given element.
   * (Concrete version.)
   */
  public ImmutableHashOrderSet<T> addElement(T elem) {
    if (inverse) {
      return new ImmutableHashOrderSet<T>(true, SortedArray.removeElement(elements,elem));
    } else {
      return new ImmutableHashOrderSet<T>(false, SortedArray.addElement(elements,elem));
    }
  }

  @SuppressWarnings("unchecked")
  public ImmutableHashOrderSet<T> addElements(Iterator<T> it) {
    List<T> l = (List<T>) tempList.get();      
    if (l != null) {
       tempList.remove();
    } else {
    	l = new ArrayList<T>();
    }
    while (it.hasNext()) {
    	l.add(it.next());
    }
    
    try {
      return union(new ImmutableHashOrderSet<T>((T[]) l.toArray()));
    } 
    finally {
      l.clear();
      tempList.set(l); 
    }
  }

  /**
   * Remove an element from the set.  Unsupported.
   * @throws UnsupportedOperationException Always thrown
   * @see #removeElement(Object)
   */
  @Override
  @Starts("nothing")
public boolean remove( final Object elem )
  {
    throw new UnsupportedOperationException( getClass().getName()
                                           + " does not support remove()" );
  }

  /**
   * Return a new set that does not include the given element.
   */
  @Override
  public ImmutableSet<T> removeCopy(T elem) {
    return removeElement(elem);
  }

  /**
   * Return a new set that does not include the given element.
   * (Concrete version)
   */
  public ImmutableHashOrderSet<T> removeElement(T elem) {
    if (inverse) {
      return new ImmutableHashOrderSet<T>(true,SortedArray.addElement(elements,elem));
    } else {
      return new ImmutableHashOrderSet<T>(false,SortedArray.removeElement(elements,elem));
    }
  }

  /** Return an immutable hash order set with these elements. */
  @SuppressWarnings("unchecked")
  protected static <T> ImmutableHashOrderSet<T> asThis(Set<T> other) {
    if (other instanceof ImmutableHashOrderSet)
      return (ImmutableHashOrderSet<T>)other;
    // assume not infinite:
    return new ImmutableHashOrderSet<T>((T[]) other.toArray());
  }

  /** Return a new set that includes all the elements of this
   * set and the argument.
   */
  @Override
  public ImmutableSet<T> union(Set<T> other) {
    return union(asThis(other));
  }

  /** Return a new set that includes all the elements of this
   * set and the argument.
   * (Concrete version.)
   */
  public ImmutableHashOrderSet<T> union(ImmutableHashOrderSet<T> other) {
	final T[] newElements;
    if (inverse) {
      if (size() == 0) return this;
      
      if (other.inverse) {
    	newElements = SortedArray.intersect(elements,other.elements);
      } else {
    	newElements = SortedArray.difference(elements,other.elements);
      }
      return new ImmutableHashOrderSet<T>(newElements, true);
    } else {
      if (size() == 0) return other;
      if (other.inverse) {
    	newElements = SortedArray.difference(other.elements,elements);
      } else {
    	newElements = SortedArray.union(elements,other.elements);
      }
      return new ImmutableHashOrderSet<T>(newElements, other.inverse);
    }
  }

  /** Return a set of all elements in both this set
   * and the argument.
   */
  @Override
  public ImmutableSet<T> intersection(Set<T> other) {
    return intersect(asThis(other));
  }

  /** Return a set of all elements in both this set
   * and the argument.
   * (Concrete Version.)
   */
  public ImmutableHashOrderSet<T> intersect(ImmutableHashOrderSet<T> other) {
	final T[] newElements;
    if (inverse) {
      if (size() == 0) return other;
      if (other.inverse) {
    	newElements = SortedArray.union(elements,other.elements);
      } else {
    	newElements = SortedArray.difference(other.elements,elements);
      }
      return new ImmutableHashOrderSet<T>(newElements, other.inverse);
    } else {
      if (size() == 0) return this;
      if (other.inverse) {
    	newElements = SortedArray.difference(elements,other.elements);
      } else {
    	newElements = SortedArray.intersect(elements,other.elements);
      }
      return new ImmutableHashOrderSet<T>(newElements, false);
    }
  }

  /** Return a set of all elements in this set that are not in
   * the argument.
   */
  @Override
  public ImmutableSet<T> difference(Set<T> set) {
    return difference(asThis(set));
  }

  /** Return a set of all elements in this set that are not in
   * the argument.
   * (Concrete Version.)
   */
  public ImmutableHashOrderSet<T> difference(ImmutableHashOrderSet<T> other) {
	final T[] newElements;
    if (inverse) {
      if (size() == 0) return other.invert();
      if (other.inverse) {
    	newElements = SortedArray.difference(other.elements,elements);
      } else {
    	newElements = SortedArray.union(elements,other.elements);
      }
      return new ImmutableHashOrderSet<T>(newElements, !other.inverse);
    } else {
      if (size() == 0) return this;
      if (other.inverse) {
    	newElements = SortedArray.intersect(elements,other.elements);
      } else {
    	newElements = SortedArray.difference(elements,other.elements);
      }
      return new ImmutableHashOrderSet<T>(newElements, false);
    }
  }

  @Override
  public String toString() {
    String contents = SortedArray.toString(elements);
    if (inverse)
      return "~" + contents;
    else
      return contents;
  }

  
  private static ThreadLocal<List<?>> tempList = new ThreadLocal<List<?>>() {
	  @Override
	  protected List<?> initialValue() {
		  return new ArrayList<Object>();
	  }
  };
}

/** Routines for handling arrays of objects sorted by hashCode.
 * All routines are static, because we can't inherit from Object[]
 * 
 */
@SuppressWarnings("all")
class SortedArray {
  static final Object[] empty = SLUtility.EMPTY_OBJECT_ARRAY;

  static <V> V[] empty() {
    return (V[]) empty;
  }
  
  public static void main(String[] args) {
	Random r = new Random();
	for(int t=0; t<100; t++) {
		int num = r.nextInt(100000); 
		System.out.println("Sorting "+num+" elements");
		Object[] a = new Object[num];
		for(int i=0; i<a.length; i++) {
			a[i] = new Object();
		}
		Object[] b = new Object[num];
		for(int i=0; i<a.length; i++) {
			b[i] = a[i];
		}

		final long start = System.currentTimeMillis();
		sort(a);
		System.out.println("Total time (in ms) = "+
				(System.currentTimeMillis() - start));

		final long start2 = System.currentTimeMillis();
		sort2(b);
		System.out.println("Total time (in ms) = "+
				(System.currentTimeMillis() - start2));

		for(int i=0; i<a.length; i++) {
			if (b[i] != a[i]) {
				System.out.println("Element "+i+" is not the same");
			}
		}
	}
  }
  
  /** Sort in place the contents of an unsorted array. */
  static void sort3(Object a[]) {
    //! right now, use simple bubble sort.
    boolean done;
    do {
      done = true;
      for (int i=1; i < a.length; ++i) {
	//! we should cache the calls to hashCode
	if (a[i].hashCode() < a[i-1].hashCode()) {
	  Object tmp = a[i];
	  a[i] = a[i-1];
	  a[i-1] = tmp;
	  done = false;
	}
      }
    } while (!done);
  }

  static final Comparator hashCompare = new Comparator() {
      public int compare(Object o1, Object o2) {
    	  return (o1.hashCode() - o2.hashCode());
      }
    };

  static <V> void sort2(V[] a) {
    Arrays.sort(a, hashCompare);
  }

  static <V> V[] sort(V[] a) {
    int n = a.length;
    if (n <= 1) return a;
    boolean sorted = true;
    int prev = a[0].hashCode();
    for (int i=1; sorted && i < n; ++i) {
      int h = a[i].hashCode();
      if (h < prev) sorted = false;
      else prev = h;
    }
    if (sorted) return a;
    V[] aux = cloneArray(a);
    if (aux.length < a.length) {
      System.out.println("aux is shorter than a");
    }
    mergeSort(aux, a, 0, a.length);
    cacheArray(aux);
    return a;
  }

  /*
  private static final int MAX_ARRAYS = 40;
  private static Object[][] cachedArrays = new Object[MAX_ARRAYS][];
  static {
    for(int i=0; i<MAX_ARRAYS; i++) {
      cachedArrays[i] = new Object[i];
    }
  }
  */
  
  private static ThreadLocal<Object[]> lastArray = new ThreadLocal<Object[]>() {
	  protected Object[] initialValue() {
		  return new Object[300];
	  }
  }; 
  
  static void clearCaches() {
	  lastArray.remove();
	  tempList.remove();
  }
  
  // Only used by sort()
  private static <V> V[] cloneArray(V[] a) {
    final int len = a.length;
    V[] array = empty();
    
    try {
      /*
      if (len < MAX_ARRAYS && cachedArrays[len] != null) {
        array = cachedArrays[len];
        cachedArrays[len] = null;

      	System.arraycopy(a, 0, array, 0, a.length);
	      return array;

      }
      else 
      */
      V[] last = (V[]) lastArray.get();
      if (last != null && len <= last.length) {
    	array = last;
    	lastArray.remove();
        
        System.arraycopy(a, 0, array, 0, a.length);
        return array;  
      }
    } catch(ArrayStoreException e) {
      SLLogger.getLogger().log(Level.SEVERE,
					"Trying to copy a " + a + " to a " + array, e);
      return clone(a);
    }
    final Logger log = SLLogger.getLogger();
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, "Need a array of length " + a.length);
	}
    // Returns an array of whatever type is passed in
    //return a.clone();
    return clone(a);
  }
  
  /**
   * Created to make sure that the returned array can truly
   * take any type
   */
  private static <V> V[] clone(V[] a) {
	  V[] clone = (V[]) new Object[a.length];
      System.arraycopy(a, 0, clone, 0, a.length);
	  return clone;
  }
  
  // Only used by sort()
  private static <V> void cacheArray(V[] a) {
    /*
    if (a.length < MAX_ARRAYS) {
      cachedArrays[a.length] = a;
    } else {
      lastArray = a;
    }
    */
	//Arrays.fill(a, null);
    lastArray.set(a);
  }  
  private static final int SHELL_INCREMENT = 4;
  
  // Copied from java.util.Arrays, and customized
  // Made to assume that dest is the correct length
  // src can be longer with unused elements
  static void mergeSort(Object src[], Object dest[], int low, int high) {
	  final int length = high - low;
	  // Insertion sort on smallest arrays
	  if (length < 7) {
		  /* Original
	      for (int i = low; i < high; i++)
	      for (int j = i; j > low
	      && dest[j - 1].hashCode() > dest[j].hashCode(); j--)
	      swap(dest, j, j - 1);
		   */
		  // Shell sort pass with SHELL_INCREMENT
		  for (int i = low+SHELL_INCREMENT, j=low; i < high; i++, j++) {
			  final int iHash = dest[i].hashCode(); 

			  // Find out if dest[i] should be somewhere else
			  final int jHash = dest[j].hashCode();
			  if (jHash > iHash) {
				  swap(dest, i, j);		
			  }
		  }
		  
		  // i is the element being inserted into the sorted part of the array
		  // dest[low] is already "sorted" by default
		  for (int i = low+1; i < high; i++) {
			  final int iHash = dest[i].hashCode(); 

			  // Find out if dest[i] should be somewhere else
			  for (int j = i; j > low
			  && dest[j - 1].hashCode() > iHash; j--) 
			  {
				  swap(dest, j, j - 1);			
			  }
		  }
		  return;
	  }
	  // Recursively sort halves of dest into src
	  final int mid = (low + high) / 2;
	  mergeSort(dest, src, low, mid);
	  mergeSort(dest, src, mid, high);
	  // If list is already sorted, just copy from src to dest. This is an
	  // optimization that results in faster sorts for nearly ordered lists.
	  if (src[mid - 1].hashCode() <= src[mid].hashCode()) {
		  System.arraycopy(src, low, dest, low, length);
		  return;
	  }
	  // Merge sorted halves (now in src) into dest
	  /* Original
       for (int i = low, p = low, q = mid; i < high; i++) {
       if (q >= high || p < mid && src[p].hashCode() <= src[q].hashCode())
       dest[i] = src[p++];
       else
       dest[i] = src[q++];
       }
	   */
	  // Based on initial values of p and q
	  int pHash = src[low].hashCode();
	  int qHash = src[mid].hashCode();
	  final int len = (src.length < dest.length) ? src.length : dest.length;

	  for (int i = low, p = low, q = mid; i < high; i++) {
		  if (q >= high || p < mid && pHash <= qHash) {
			  dest[i] = src[p++]; 
			  pHash   = src[p].hashCode(); // updated whenever p changes 
		  } else {
			  dest[i] = src[q++];
			  qHash   = (q < len /*Same as src.length*/) ? src[q].hashCode() : 0;
		  }
	  }		
  }
  
  /**
   * Remove duplicates from a hash sorted array. Return the parameter if no
   * duplicates found. Otherwise returns a new array. The parameter is also
   * updated to have the unique elements in the first return.length elements.
   * (The remaining ones are set to null which will cause methods in this class
   * to crash.)
   */
  static <V> V[] uniq(V[] x) {
    final int n = x.length;
    int to = 0;
    int firsteq = 0;
    int h = 0;
  move_loop : 
    for (int from = 0; from < n; ++from) {
      /*
       * @invariant \forall i: firsteq <= i && i < to \implies
       * x[i].hashCode() == h
       */
      final int fromh = x[from].hashCode();
      if (fromh == h) {
	// perhaps not unique
	for (int i = firsteq; i < to; ++i) {
	  if (x[i].equals(x[from])) {
	    continue move_loop; // this element is definitely not unique
	  }
	}
	// must be unique (but has same hash code)
      } else {
	// must be unique, and can start a new range
	firsteq = to;
	h = fromh;
      }
      x[to] = x[from];
      ++to;
    }
    if (to == n) {
      return x;
    }
    final V[] result = (V[]) new Object[to];
    /*
      for (int i = 0; i < to; ++i) {
      result[i] = x[i];
      }
    */
    System.arraycopy(x, 0, result, 0, to);
    /*
      for (int i = to; i < n; ++i) {
      x[i] = null;
      }
    */
    Arrays.fill(x, to, n, null);
    return result;
  }
  
  /**
   * Swaps x[a] with x[b].
   */
  private static void swap(Object x[], int a, int b) {
    Object t = x[a];
    x[a] = x[b];
    x[b] = t;
  }
  
  static <V> int hashCode(V[] a) {
    int h = 0;
    for (int i=0; i < a.length; ++i) {
      h += a[i].hashCode();
    }
    return h;
  }

  static <V> Enumeration<V> elements(V[] a) {
    return new SortedArrayEnumeration<V>(a);
  }
  
  static <T> Iterator<T> iterator( T[] a )
  {
    return new SortedArrayIterator( a );
  }

  /** Return true if the element is found inside the sorted array. */
  static <V> boolean contains(V[] a, Object elem) {
    if (a.length == 0) return false;
    if (elem == null) return false;
    int h = elem.hashCode();
    // We use a modified version of binary search
    // to find two points: start and stop
    // a[j].hashCode >= h  ==>  j >= start
    // a[j].hashCode <= h  ==>  j < stop
    // and furthermore start is the largest it can be
    // and stop is the smallest it can be.

    int startlow = 0;
    int starthigh = a.length;
    while (starthigh > startlow) {
      int mid = (starthigh+startlow)/2; // == startlow if one separate
      int m = a[mid].hashCode();
      if (m < h) {
	startlow = mid+1;
      } else {
	starthigh = mid;
      }
    }
    // NB: starthigh == 0 and starthigh == a.length are both possible
    if (starthigh == a.length) return false; // all are smaller

    int stoplow = starthigh;
    int stophigh = a.length;
    while (stophigh > stoplow) {
      int mid = (stophigh+stoplow)/2; // == stoplow if one separate
      int m = a[mid].hashCode();
      if (m <= h) {
	stoplow = mid+1;
      } else {
	stophigh = mid;
      }
    }

    for (int i=starthigh; i < stoplow; ++i) {
      if (a[i].equals(elem)) return true;
    }

    return false;
  }

  static <V> boolean equals(V[] a, V[] b) {
    if (a == b) return true;
    if (a.length != b.length)	return false;
    int low = 0;
    while (low < a.length) {
      final int h = a[low].hashCode();
      int high = low + 1;
      // Find out the range of elements in a with the same hash code
      for (; high < a.length && a[high].hashCode() == h; ++high) { /*loop*/ }

      
      // NB: common case high = low+1
      // In this situation, each loop has only one iteration
      // For each duplicate hash code ...
    outer:
      for (int i = low; i < high; ++i) {
	/*
	  boolean found = false;
	  // Find the element in b that matches
	  for (int j = low; !found && j < high; ++j) {
	  if (a[i].equals(b[j])) found = true;					
	  }
	  if (!found)
	  return false;
	*/
				// Find the element in b that matches
	for (int j = low; j < high; ++j) {
	  if (a[i].equals(b[j])) {
	    continue outer; // Found, so keep checking
	  }
	} 
	return false; // Not found, so not equal
      }
      low = high;
    }
    return true;
  }
  
  static <V> boolean includes(V[] a, V[] b) {
    if (a == b || b.length == 0) return true;
    if (a.length < b.length) return false;
    if (b.length == 1) return contains(a,b[0]);
    // Otherwise, use O((A+B)H) algorithm where H is the
    // the hash duplication factor (1 for perfect hashes).
    int lowa = 0;
    int lowb = 0;
    while (lowa < a.length && lowb < b.length) {
      int h = b[lowb].hashCode();
      for (; lowa < a.length && a[lowa].hashCode() < h; ++lowa) { /*loop*/ }
      int higha = lowa;
      for (; higha < a.length && a[higha].hashCode() == h; ++higha) { /*loop*/ }
      if (higha == lowa) return false; // no elements match
      int highb = lowb+1;
      for (; highb < b.length && b[highb].hashCode() == h; ++highb) { /*loop*/ }
      if (higha-lowa < highb-lowb) return false; // not enough elements match
      // NB: common case highX = lowX+1
      // In this situation, each loop has only one iteration
      for (int i=lowb; i < highb; ++i) {
	boolean found = false;
	for (int j = lowa; !found && j < higha; ++j) {
	  if (a[j].equals(b[i])) found = true;
	}
	if (!found) return false;
      }
      lowa = higha;
      lowb = highb;
    }
    return lowb == b.length;
  }

  static <V> boolean overlaps(V[] a, V[] b) {
    if (a == b) return a.length != 0;
    int lowa = 0;
    int lowb = 0;
    while (lowa < a.length && lowb < b.length) {
      int ah = a[lowa].hashCode();
      int bh = b[lowb].hashCode();
      if (ah == bh) {
	int higha = lowa;
	int highb = lowb;
	for (higha = lowa+1;
	     higha < a.length && a[higha].hashCode() == ah;
	     ++higha) { /*loop*/ }
	for (highb = lowb+1;
	     highb < b.length && b[highb].hashCode() == ah;
	     ++highb) { /*loop*/ }
	for (int i=lowb; i < highb; ++i) {
	  for (int j = lowa; j < higha; ++j) {
	    if (a[j].equals(b[i])) return true;
	  }
	}
	lowa = higha;
	lowb = highb;
      } else if (ah < bh)  {
	// move lowa along
	for (; lowa < a.length && a[lowa].hashCode() < bh; ++lowa) { /*loop*/ }
      } else {
	// move lowb along
	for (; lowb < b.length && b[lowb].hashCode() < ah; ++lowb) { /*loop*/ }
      }
    }
    return false;
  }

  static <V> V[] addElement(V[] a, V elem) {
    if (contains(a,elem)) return a;
    V[] b = (V[]) new Object[a.length+1];
    int i=0;
    int h = elem.hashCode();
    while (i<a.length) {
      if (a[i].hashCode() >= h) {
	break;
      }
      b[i] = a[i];
      ++i;
    }
    b[i] = elem;
    while (i<a.length) {
      b[i+1] = a[i];
      ++i;
    }
    return b;
  }

  static <V> V[] removeElement(V[] a, Object elem) {
    if (!contains(a,elem)) return a;
    V[] b = (V[]) new Object[a.length-1];
    int j=0;
    int i=0;
    while (i<a.length) {
      if (a[i].equals(elem)) {
	break;
      }
      b[i] = a[i];
      ++i;
    }
    ++i;
    while (i<a.length) {
      b[i-1] = a[i];
      ++i;
    }
    return b;
  }

  public static final int A_EXCL = 1;
  public static final int B_EXCL = 2;
  public static final int OVERLAP = 4;
  public static final int A_INCL = A_EXCL|OVERLAP;
  public static final int B_INCL = B_EXCL|OVERLAP;
  
  private static ThreadLocal<List> tempList = new ThreadLocal<List>();
  
  static <V> V[] combine(final V[] a, final V[] b , final int flags) {	 
    List l = tempList.get();
    if (l == null) {
    	l = new ArrayList(a.length + b.length);
    } else {
        tempList.remove();      
    }

    final boolean b_EXCL = (flags & B_EXCL) == B_EXCL;
	final boolean a_INCL = (flags & A_INCL) == A_INCL;
	final boolean only_OVERLAP = (flags & OVERLAP) == OVERLAP && !a_INCL;
    int lowa = 0;
    int lowb = 0;
    while (lowa < a.length && lowb < b.length) {
      int ah = a[lowa].hashCode();
      int bh = b[lowb].hashCode();
      if (ah == bh) {

        int higha, highb;
        /* move through a's elements adding if guaranteed to want them */
        for (higha = lowa; higha < a.length
        && a[higha].hashCode() == ah; ++higha) {
          if (a_INCL) {
            l.add(a[higha]);
          }
        }
        for (highb = lowb; highb < b.length
        && b[highb].hashCode() == bh; ++highb) {
          /*
           * only if the elements are potentially relevant do we see
           * if it is found
           */
          if (only_OVERLAP || b_EXCL) {
            boolean found = false;
            Object elem = b[highb];
            for (int i = lowa; !found && i < higha; ++i) {
              found = (a[i].equals(elem));
            }
            if ((found && only_OVERLAP) || (!found && b_EXCL)) {
              l.add(elem);
            }
          }
        }
        /* now if we need A_EXCL, we make a separate loop */
        if ((flags & A_EXCL) == A_EXCL && (flags & A_INCL) != A_INCL) {
          for (int i = lowa; i < higha; ++i) {
            boolean found = false;
            Object elem = a[i];
            for (int j = lowb; !found && j < highb; ++j) {
              found = elem.equals(b[j]);
            }
            if (!found)
              l.add(elem);
          }
        }
        lowa = higha;
        lowb = highb;
      } else if (ah < bh) {
        // move lowa along
    	final boolean a_EXCL = (flags & A_EXCL) == A_EXCL;
        for (; lowa < a.length && a[lowa].hashCode() < bh; ++lowa) {
          if (a_EXCL)
            l.add(a[lowa]);
        }
      } else { // ah > bh
        // move lowb along
        for (; lowb < b.length && b[lowb].hashCode() < ah; ++lowb) {
          if (b_EXCL)
            l.add(b[lowb]);
        }
      }
    }
    /* finish off both sequences */
    if ((flags & A_EXCL) == A_EXCL) {
      for (; lowa < a.length; ++lowa) {
        l.add(a[lowa]);
      }
    }
    if ((flags & B_EXCL) == B_EXCL) {
      for (; lowb < b.length; ++lowb) {
        l.add(b[lowb]);
      }
    }
    try {
      if (l.size() == 0)
        return empty();
      /*
      if (a.length > 20 || b.length > 20) {
    	  System.out.println("a = "+a.length+", b = "+b.length+" -> "+l.size());
      }
      */
      return (V[]) l.toArray();
    }
    finally {
      l.clear();
      tempList.set(l);
    }
  }    
  
  static <V> V[] union(V a[], V b[]) {
    // first, try to avoid creating a new set
    if (includes(b,a)) return b;
    if (includes(a,b)) return a;
    return combine(a,b,A_INCL|B_INCL);
  }

  static <V> V[] intersect(V a[], V b[]) {
    // first, try to avoid creating a new set
    if (includes(b,a)) return a;
    if (includes(a,b)) return b;
    return combine(a,b,OVERLAP);
  }

  static <V> V[] difference(V[] a, V[] b) {
    // first, try to avoid creating a new set
    if (includes(b,a)) return empty();
    if (!overlaps(a,b)) return a;
    return combine(b,a,B_EXCL); // B_EXCL is more efficient
  }

  static <V> V[] symmetricDifference(V a[], V b[]) {
    return combine(a,b,A_EXCL|B_EXCL);
  }

  public static <V> String toString(V a[]) {
    StringBuilder sb = new StringBuilder("{");
    for (int i=0; i < a.length; ++i) {
      if (i != 0) sb.append(",");
      sb.append(a[i].toString());
    }
    sb.append("}");
    return sb.toString();
  }
}
 
class SortedArrayEnumeration<V> implements Enumeration<V> {
  V[] array;
  int index = 0;
  SortedArrayEnumeration(V[] a) {
    array = a;
  }

  @Override
  public boolean hasMoreElements() {
    return index < array.length;
  }

  @Override
  public V nextElement() throws NoSuchElementException {
    if (index >= array.length)
      throw new NoSuchElementException("set enumeration exhausted");
    return array[index++];
  }
}

class SortedArrayIterator<V>
extends AbstractRemovelessIterator<V>
{
  V[] array;
  int index = 0;
  SortedArrayIterator(V[] a) {
    array = a;
  }

  @Override
  public boolean hasNext() {
    return index < array.length;
  }

  @Override
  public V next() throws NoSuchElementException {
    if (index >= array.length)
      throw new NoSuchElementException("set iterator exhausted");
    return array[index++];
  }
}
