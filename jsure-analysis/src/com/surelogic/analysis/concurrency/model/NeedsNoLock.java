package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public final class NeedsNoLock extends AbstractNeededLock {

  public NeedsNoLock(final IRNode source) {
    super(source, false);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * source.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof NeedsNoLock) {
      final NeedsNoLock o = (NeedsNoLock) other;
      return this.source.equals(o.source);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<none>";
  }
}
