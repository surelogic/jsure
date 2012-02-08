package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Triple;
import edu.uwm.cs.fluid.util.FlatLattice2.Element;

public final class Store
extends Triple<Element<Integer>,
    ImmutableSet<ImmutableHashOrderSet<Object>>,
    ImmutableSet<FieldTriple>> {
  public Store(final Element<Integer> size,
      final ImmutableSet<ImmutableHashOrderSet<Object>> objects,
      final ImmutableSet<FieldTriple> edges) {
    super(size, objects, edges);
    /*if (!invariant()) {
    	throw new AssertionError("invariant error");
    }*/
    assert invariant() : "invariant error"; // turn off this check for production use
  }
  
  /**
   * Return false and print out string.
   * This makes it easier to report invariant errors.
   */
  private boolean report(String s) {
	  System.err.println("Invariant error specifics: " + s);
	  return false;
  }
  
  /**
   * Make sure that the store makes sense:
   * <ul>
   * <li> Stack size must be non-negative
   * <li> All objects must be finite
   * <li> Every pseudo-variable must be present
   * <li> Every integer must be in range (1,N) where N is the stack size
   * <li> Every object mentioned in a triple must be in the object set
   * <li> The empty object must not be the destination of a triple
   * </ul>
   */
  boolean invariant() {
	  if (!isValid()) return true; // don't check error stores
	  int n = getStackSize();
	  if (n < 0) return report("Store invalid: stack size negative: " + n);
	  ImmutableSet<ImmutableHashOrderSet<Object>> objects = getObjects();
	  Set<Object> found = new HashSet<Object>();
	  for (ImmutableHashOrderSet<Object> obj : objects) {
		  // this will throw an exception if obj is infinite
		  for (Object v : obj) {
			  if (v instanceof Integer) {
				  int n1 = ((Integer)v).intValue();
				  if (n1 <= 0 || n1 > n) return report("Store invalid: stack slot sticking around: " + n);
			  } else found.add(v);
		  }
	  }
	  // don't require READONLY because UniqueWrite removes temporarily
	  for (Object s : new Object[]{State.UNDEFINED,State.BORROWED,State.SHARED,StoreLattice.VALUE, StoreLattice.NONVALUE}) {
		  if (!found.contains(s)) {
			  System.out.println(StoreLattice.nodeToString(new ImmutableHashOrderSet<Object>(found)));
			  return report("Did not find " + s + " in objects");
		  }
	  }
	  for (FieldTriple tp : getFieldStore()) {
		  if (!objects.contains(tp.first())) {
			  return report("source of field is not in objects: " + tp.first());
		  }
		  if (tp.third().isEmpty()) return report("empty object is dest of field.");
		  if (!objects.contains(tp.third())) {
			  return report("dest of field is not in objects: " + tp.third());
		  }
	  }
	  return true;
  }
  
  public Integer getStackSize() {
    return first().getValue();
  }
  
  public ImmutableSet<ImmutableHashOrderSet<Object>> getObjects() {
    return second();
  }
  
  public ImmutableSet<FieldTriple> getFieldStore() {
    return third();
  }
  
  public boolean isValid() {
    return first().inDomain() &&
      !getObjects().isInfinite() &&
      !getFieldStore().isInfinite();
  }
}
