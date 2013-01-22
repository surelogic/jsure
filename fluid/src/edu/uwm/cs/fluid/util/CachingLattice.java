package edu.uwm.cs.fluid.util;

import java.util.HashMap;

public abstract class CachingLattice<E> extends AbstractLattice<E> {
  private HashMap<Wrapper<E>,Wrapper<E>> cache = new HashMap<Wrapper<E>,Wrapper<E>>(); // it's too bad Hashmap doesn't have what we want
  
  private E topElement;
  private E bottomElement;
  
  public E cache(E v) {
    Wrapper<E> wrapped = new Hashor.Wrapper<E>(this,v);
    Wrapper<E> prev = cache.get(wrapped);
    if (prev == null) {
      prev = wrapped;
      cache.put(prev,prev);
    }
    return prev.value;
  }

  @Override
  public final E top() {
    if (topElement == null) {
      topElement = cache(computeTop());
    }
    return topElement;
  }

  @Override
  public final E bottom() {
    if (bottomElement == null) {
      bottomElement = cache(computeBottom());
    }
    return bottomElement;
  }

  @Override
  public final E join(E v1, E v2) {
    if (v1 == bottomElement) return v2;
    if (v2 == bottomElement) return v1;
    if (v1 == topElement || v2 == topElement) return topElement;
    if (lessEq(v1,v2)) return v2;
    if (lessEq(v2,v1)) return v1;
    return cache(computeJoin(v1,v2));
  }

  @Override
  public final E meet(E v1, E v2) {
    if (v1 == topElement) return v2;
    if (v2 == topElement) return v1;
    if (v1 == bottomElement || v2 == bottomElement) return bottomElement;
    if (lessEq(v1,v2)) return v1;
    if (lessEq(v2,v1)) return v2;
    return cache(computeMeet(v1,v2));
  }

  @Override
  public E widen(E v1, E v2) {
    if (v1 == bottomElement) return v2;
    if (v2 == bottomElement) return v1;
    if (v1 == topElement || v2 == topElement) return topElement;
    // for widening, we don't want this: if (lessEq(v1,v2)) return v2;
    // at least, we don't want to *mandate* it.
    if (lessEq(v2,v1)) return v1;
    return cache(computeWiden(v1,v2));
  }

  protected abstract E computeTop();
  protected abstract E computeBottom();
  protected abstract E computeMeet(E v1, E v2);
  protected abstract E computeJoin(E v1, E v2);
  
  // default implementation:
  protected E computeWiden(E v1, E v2) {
    return super.widen(v1,v2);
  }
}

