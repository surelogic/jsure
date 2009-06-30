/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/StackLattice.java,v 1.5 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.util;

/** A lattice of variable length sequences of elements,
 * each of which is a lattice element.  Elements of
 * different lengths are not comparable.  Thus this lattice
 * satisfies the ascending chain condition, as long as the
 * component lattice satisfies the same condition.
 * @see ArrayLattice
 */
public class StackLattice<T> implements Lattice<T> {
  final StackLatticeInfo<T> info;

  /** Create a stack lattice.  This will be the top element.
   * @param el the lattice for elements
   */
  public StackLattice(Lattice<T> el) {
    info = new StackLatticeInfo<T>(this,el);
  }

  protected StackLattice(StackLatticeInfo<T> i) {
    info = i;
  }

  public final Lattice<T> top() {
    return info.top;
  }

  public final Lattice<T> bottom() {
    return info.bottom;
  }
  
  public Lattice<T> meet(Lattice<T> other) {
    return other;
  }

  public boolean includes(Lattice<T> other) {
    return true;
  }

  /**
   * Return the empty stack as a lattice value.
   */
  public final StackLattice<T> empty() {
    return info.empty;
  }

  /** Return a new stack lattice
   * value after pushing an element.
   * Strict.
   */
  public StackLattice<T> push(Lattice<T> e) {
    return this;
  }

  /** Get the lattice value for the top of the stack.
   * Not to be confused with @{link #top()} which returns the
   * top of the lattice.
   */
  public Lattice<T> peek() {
    return info.elementLattice.top();
  }

  /** Return a new stack lattice value after popping
   * the top element.  If the stack is empty, we return bottom.
   * Strict.
   */
  public StackLattice<T> pop() {
    return this;
  }

  // don't override Object.toString
}

class StackLatticeInfo<T> {
  final StackLattice<T> top;
  final BottomStackLattice<T> bottom;
  final EmptyStackLattice<T> empty;
  final Lattice<T> elementLattice;

  StackLatticeInfo(StackLattice<T> t, Lattice<T> el) {
    elementLattice = el;
    top = t;
    bottom = new BottomStackLattice<T>(this);
    empty = new EmptyStackLattice<T>(this);
  }
}

class BottomStackLattice<T> extends StackLattice<T> {
  BottomStackLattice(StackLatticeInfo<T> i) {
    super(i);
  }

  @Override
  public Lattice<T> meet(Lattice<T> other) {
    return this;
  }

  @Override
  public boolean includes(Lattice<T> other) {
    return other == this;
  }

  @Override
  public Lattice<T> peek() {
    return info.elementLattice.bottom();
  }

  @Override
  public String toString() {
    return "BOT";
  }
}

class EmptyStackLattice<T> extends StackLattice<T> {
  EmptyStackLattice(StackLatticeInfo<T> i) {
    super(i);
  }

  @Override
  public Lattice<T> meet(Lattice<T> other) {
    if (other == this || other == top()) return this;
    return bottom();
  }

  @Override
  public boolean includes(Lattice<T> other) {
    return other == this || other == bottom();
  }

  @Override
  public StackLattice<T> pop() {
    return info.bottom;
  }

  @Override
  public StackLattice<T> push(Lattice<T> e) {
    return new NonEmptyStackLattice<T>(e,this);
  }

  @Override
  public Lattice<T> peek() {
    return info.elementLattice.bottom();
  }

  @Override
  public String toString() {
    return "<>";
  }
}

class NonEmptyStackLattice<T> extends StackLattice<T> {
  Lattice<T> element;
  StackLattice<T> remainder;

  NonEmptyStackLattice(Lattice<T> e, StackLattice<T> rest) {
    super(rest.info);
    element = e;
    remainder = rest;
  }

  @Override
  public StackLattice<T> pop() {
    return remainder;
  }

  @Override
  public StackLattice<T> push(Lattice<T> e) {
    return new NonEmptyStackLattice<T>(e,this);
  }

  @Override
  public Lattice<T> peek() {
    return element;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof NonEmptyStackLattice)) return false;
    NonEmptyStackLattice nsl = (NonEmptyStackLattice)other;
    return element.equals(nsl.element) &&
      remainder.equals(nsl.remainder);
  }

  @Override
  public int hashCode() {
    // presumably rarely used:
    return element.hashCode() + remainder.hashCode();
  }

  @Override
  public boolean includes(Lattice<T> l) {
    if (l == bottom() || l == this) return true;
    if (!(l instanceof NonEmptyStackLattice)) return false;
    NonEmptyStackLattice<T> nsl = (NonEmptyStackLattice<T>)l;
    return element.includes(nsl.element) &&
      remainder.includes(nsl.remainder);
  }

  @Override
  public Lattice<T> meet(Lattice<T> l) {
    if (l == top() || l == this) return this;
    if (!(l instanceof NonEmptyStackLattice)) return bottom();
    NonEmptyStackLattice<T> nsl = (NonEmptyStackLattice<T>)l;
    Lattice<T> e = element.meet(nsl.element);
    StackLattice<T> rm = (StackLattice<T>)remainder.meet(nsl.remainder);
    if (e == element && rm == remainder) return this;
    if (e == nsl.element && rm == nsl.remainder) return nsl;
    return rm.push(e);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<"+element);
    StackLattice sl = remainder;
    while (sl instanceof NonEmptyStackLattice) {
      NonEmptyStackLattice nesl = (NonEmptyStackLattice)sl;
      sb.append(","+nesl.element);
      sl = nesl.remainder;
    }
    sb.append(">");
    return sb.toString();
  }
}
