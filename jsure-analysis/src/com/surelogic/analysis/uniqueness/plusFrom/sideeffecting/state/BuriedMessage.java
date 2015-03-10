package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.Messages;
import com.surelogic.common.SLUtility;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

public enum BuriedMessage {
  VAR(Messages.READ_OF_BURIED) {
    @Override
    public Object[] getVarArgs(final Object v) {
      return SLUtility.EMPTY_OBJECT_ARRAY;
    }    
  },
  
  RETURN(Messages.RETURN_OF_BURIED) {
    @Override
    public Object[] getVarArgs(final Object v) {
      return SLUtility.EMPTY_OBJECT_ARRAY;
    }    
  },
  
  EXTERNAL_VAR(Messages.READ_OF_BURIED_EXTERNAL) {
    @Override
    public Object[] getVarArgs(final Object v) {
      return new Object[] { VariableDeclarator.getId((IRNode) v) };
    }    
  };

  private final int msg;
  
  private BuriedMessage(final int m) {
    msg = m;
  }
  
  public final int getMessage() {
    return msg;
  }
  
  public abstract Object[] getVarArgs(Object v);
}
