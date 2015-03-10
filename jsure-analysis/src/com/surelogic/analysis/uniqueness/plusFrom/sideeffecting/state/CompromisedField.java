package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;

import edu.cmu.cs.fluid.ir.IRNode;

final class CompromisedField {
  public final State fieldState;
  public final IRNode srcOp;
  
  private final int hashCode;
  
  public CompromisedField(final State fs, final IRNode src) {
    fieldState = fs;
    srcOp = src;
    
    int hc = 17;
    hc = 31 * hc + fieldState.hashCode();
    hc = 31 * hc + srcOp.hashCode();
    hashCode = hc;
  }
  
  @Override
  public int hashCode() { return hashCode; }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof CompromisedField) {
      final CompromisedField other = (CompromisedField) o;
      return fieldState == other.fieldState &&
          srcOp.equals(other.srcOp);
    }
    return false;
  }
}
