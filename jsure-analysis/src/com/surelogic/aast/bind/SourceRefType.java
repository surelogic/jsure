package com.surelogic.aast.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

public class SourceRefType extends Type implements ISourceRefType {
  protected final IBinder eb;
  
  protected SourceRefType(IRNode t, IBinder b) {
    super(t);
    eb = b;
  }

  @Override
  public boolean fieldExists(String id) {
    IRNode o = eb.findClassBodyMembers(type, new FindFieldStrategy(eb, id), true);       
    return o != null;
  }    
  @Override
  public IVariableBinding findField(String id) {
    final IRNode o = eb.findClassBodyMembers(type, new FindFieldStrategy(eb, id), true);      
    if (o == null) {
    	return null;
    }
    return new IVariableBinding() {
      @Override
      public IRNode getNode() {
        return o;
      }
      @Override
      public IJavaType getJavaType() {
        IRNode t = VariableDeclarator.getType(o);
        return eb.getJavaType(t);
      }
    };
  }  
}
