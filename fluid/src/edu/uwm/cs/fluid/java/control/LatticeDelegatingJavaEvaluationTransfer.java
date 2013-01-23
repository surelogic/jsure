package edu.uwm.cs.fluid.java.control;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.java.analysis.JavaEvaluationOperations;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * An abstract {@link JavaEvaluationTransfer} implementation that delegates
 * the fundamental stack operations to the lattice. 
 */
public abstract class LatticeDelegatingJavaEvaluationTransfer<L extends Lattice<T> & JavaEvaluationOperations<T, V>, T, V>
extends JavaEvaluationTransfer<L, T> {
  protected LatticeDelegatingJavaEvaluationTransfer(final IBinder binder,
      final L lattice, final SubAnalysisFactory<L, T> factory, final int floor) {
    super(binder,lattice, factory, floor);
  }

  @Override
  protected final T pop(final T val) {
    if (!lattice.isNormal(val)) return val;
    return lattice.pop(val);
  }
  
  @Override
  protected final T push(final T val) {
    return push(val, lattice.getAnonymousStackValue());
  }

  /** Push a known element onto the stack. */
  protected final T push(final T val, final V v) {
    if (!lattice.isNormal(val)) return val;
    return lattice.push(val, v);
  }
  
  @Override
  protected final T popAllPending(final T val) {
    if (!lattice.isNormal(val)) return val;
    return lattice.popAllPending(val, stackFloorSize);
  }

  @Override
  protected final T popSecond(final T val) {
    if (!lattice.isNormal(val)) return val;
    return lattice.popSecond(val);
  }

  @Override
  protected final T dup(final T val) {
    if (!lattice.isNormal(val)) return val;
    return lattice.dup(val);
  }
}
