package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.Messages;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

public enum BuriedMessage {
  VAR {
    @Override
    public int getMessage() {
      return Messages.READ_OF_BURIED;
    }

    @Override
    public Object[] getVarArgs(final Object v) {
      return new Object[0];
    }    
  },
  
  RETURN {
    @Override
    public int getMessage() {
      return Messages.RETURN_OF_BURIED;
    }

    @Override
    public Object[] getVarArgs(final Object v) {
      return new Object[0];
    }    
  },
  
  EXTERNAL_VAR {
    @Override
    public int getMessage() {
      return Messages.READ_OF_BURIED_EXTERNAL;
    }

    @Override
    public Object[] getVarArgs(final Object v) {
      return new Object[] { VariableDeclarator.getId((IRNode) v) };
    }    
  };
  
  public abstract int getMessage();
  public abstract Object[] getVarArgs(Object v);
}
