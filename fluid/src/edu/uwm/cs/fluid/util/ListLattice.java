/*
 * CompSci 552
 * Homework #3
 * Solution
 * John Boyland
 * Fall 2005
 */
package edu.uwm.cs.fluid.util;

import java.util.Iterator;

import edu.cmu.cs.fluid.util.ImmutableList;

/**
 * A lattice of values representing an unbounded series of elements.
 * Only lists of the same size are comparable.
 * It can be used as a stack lattice, if desired.
 * If you want a fixed size, use {@link ArrayLattice} instead.
 * <p>
 * This class takes the lattice type as well as the underlying
 * type as a parameter.  This only helps {@link #getBaseLattice()} have
 * a more precise type.  If this precision is not needed, then {@link L} can
 * always simply be <code>Lattice&lt;T&gt;</code>.
 * @param <L> the lattice of the list elements.
 * @param <T> the type of the list elements
 * @author boyland
 */
public class ListLattice<L extends Lattice<T>, T> extends AbstractLattice<ImmutableList<T>> {
  private final L baseLattice;
  
  // fake values that must be handled specially:
  private final ImmutableList<T> bottom = new ImmutableList<T>((T)null);
  private final ImmutableList<T> top = ImmutableList.cons(null,bottom);
  
  public ListLattice(L base) {
    baseLattice = base;
  }
  
  public L getBaseLattice() {
    return baseLattice;
  }
  
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#lessEq(E, E)
   */
  @Override
  public boolean lessEq(ImmutableList<T> v1, ImmutableList<T> v2) {
    if (v1 == bottom || v2 == top || v1 == v2) return true;
    if (v1 == top || v2 == bottom) return false;
    if (v1.size() != v2.size()) return false;
    Iterator<T> it1 = v1.iterator(), it2 = v2.iterator();
    while (it1.hasNext()) {
      if (!baseLattice.lessEq(it1.next(),it2.next())) return false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#top()
   */
  @Override
  public ImmutableList<T> top() {
    return top;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#bottom()
   */
  @Override
  public ImmutableList<T> bottom() {
    return bottom;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#join(E, E)
   */
  @Override
  public ImmutableList<T> join(ImmutableList<T> v1, ImmutableList<T> v2) {
    if (v1 == bottom || v2 == top) return v2;
    if (v1 == top || v2 == bottom) return v1;
    if (v1.isEmpty() && v2.isEmpty()) return v1;
    if (v1.isEmpty() || v2.isEmpty()) return top;
    ImmutableList<T> t1 = v1.tail();
    ImmutableList<T> t2 = v2.tail();
    ImmutableList<T> t = join(t1,t2);
    if (t == top) return t;
    T h = baseLattice.join(v1.head(),v2.head());
    if (h == v1.head() && t == t1) return v1;
    if (h == v2.head() && t == t2) return v2;
    return ImmutableList.cons(h,t);
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#meet(E, E)
   */
  @Override
  public ImmutableList<T> meet(ImmutableList<T> v1, ImmutableList<T> v2) {
    if (v1 == bottom || v2 == top) return v1;
    if (v1 == top || v2 == bottom) return v2;
    if (v1.isEmpty() && v2.isEmpty()) return v1;
    if (v1.isEmpty() || v2.isEmpty()) return bottom;
    ImmutableList<T> t1 = v1.tail();
    ImmutableList<T> t2 = v2.tail();
    ImmutableList<T> t = meet(t1,t2);
    if (t == bottom) return t;
    T h = baseLattice.meet(v1.head(),v2.head());
    if (h == v1.head() && t == t1) return v1;
    if (h == v2.head() && t == t2) return v2;
    return ImmutableList.cons(h,t);
  }

   @Override
  public ImmutableList<T> widen(ImmutableList<T> v1, ImmutableList<T> v2) {
     if (v1 == bottom || v2 == top) return v2;
     if (v1 == top || v2 == bottom) return v1;
     if (v1.isEmpty() && v2.isEmpty()) return v1;
     if (v1.isEmpty() || v2.isEmpty()) return top;
     ImmutableList<T> t1 = v1.tail();
     ImmutableList<T> t2 = v2.tail();
     ImmutableList<T> t = widen(t1,t2);
     if (t == top) return t;
     T h = baseLattice.widen(v1.head(),v2.head());
     if (h == v1.head() && t == t1) return v1;
     if (h == v2.head() && t == t2) return v2;
     return ImmutableList.cons(h,t);
  }
   
  /**
   * Return true if the argument is an actual list (and not top or bottom)
   * @param l lattice value
   * @return true if a real list
   */
  public boolean isList(ImmutableList<T> l) {
    return l != top && l != bottom;
  }
   
   // stack operations
   
  public ImmutableList<T> push(ImmutableList<T> l, T e) {
    if (l == top || l == bottom) return l;
    return ImmutableList.cons(e,l);
  }
  
  public ImmutableList<T> pop(ImmutableList<T> l) {
    if (l == top || l == bottom) return l;
    if (l.isEmpty()) return top;
    return l.tail();
  }
  
  public T peek(ImmutableList<T> l) {
    if (l == top) return baseLattice.top();
    if (l == bottom) return baseLattice.bottom();
    if (l.isEmpty()) return baseLattice.top();
    return l.head();
  }
  
  // list operations:
  
  public T get(ImmutableList<T> l, int i) {
    if (l == top) return baseLattice.top();
    if (l == bottom) return baseLattice.bottom();
    for (;;) {
      if (l.isEmpty()) return baseLattice.top();
      if (i == 0) return l.head();
      --i;
      l = l.tail();
    }
  }
  
  public ImmutableList<T> set(ImmutableList<T> l, int i, T newValue) {
    if (l == top) return top;
    if (l == bottom) return bottom;
    if (l.isEmpty()) return top;
    T h = l.head();
    ImmutableList<T> t = l.tail();
    if (i == 0) {
      if (baseLattice.equals(h,newValue)) return l;
      return push(t,newValue);
    } else {
      ImmutableList<T> newt = set(t,i-1,newValue);
      assert (newt != bottom);
      if (newt == top) return top;
      if (newt == t) return l;
      return push(t,h);
    }
  }

  @Override
  public String toString(ImmutableList<T> v) {
    if (v == bottom) return "bot";
    else if (v == top) return "top";
    else return super.toString(v);
  }

}
