package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

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
  }
  
  
  
  public Integer getStackSize() {
    return elem1.getValue();
  }
  
  public ImmutableSet<ImmutableHashOrderSet<Object>> getObjects() {
    return elem2;
  }
  
  public ImmutableSet<FieldTriple> getFieldStore() {
    return elem3;
  }
  
  public boolean isValid() {
    return first().inDomain() &&
      !getObjects().isInfinite() &&
      !getFieldStore().isInfinite();
  }
}
