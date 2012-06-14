package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;

import edu.cmu.cs.fluid.ir.IRNode;

final class CompromisedField {
  public final State fieldState;
  public final IRNode srcOp;
  
  public CompromisedField(final State fs, final IRNode src) {
    fieldState = fs;
    srcOp = src;
  }
}
