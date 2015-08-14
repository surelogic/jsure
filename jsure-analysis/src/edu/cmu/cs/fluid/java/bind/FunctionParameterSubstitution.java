/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/FunctionParameterSubstitution.java,v 1.4 2008/07/21 17:08:51 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;

public class FunctionParameterSubstitution extends AbstractTypeSubstitution {  
  private final IRNode methodDecl;
  private final List<IJavaType> actuals;
  
  private FunctionParameterSubstitution(IBinder b, IRNode md, List<IJavaType> args) {
    super(b);
    methodDecl = md;
    actuals = args;
  }

  public boolean isNull() {
	  return actuals.isEmpty();
  }
  
  @Override
  protected <V> V process(IJavaTypeFormal jtf, Process<V> processor) {
    IRNode decl = jtf.getDeclaration();
    IRNode parent = JJNode.tree.getParent(decl);
    IRNode md = JJNode.tree.getParent(parent);
    if (methodDecl.equals(md)) {
      Iterator<IRNode> ch = JJNode.tree.children(parent);
      for (IJavaType jt : actuals) {
        if (decl.equals(ch.next())) {
            return processor.process(jtf, decl, jt);
        }
      }      
    }
    return null;
  }
  
  @Override
  public String toString() {
	  StringBuilder sb = new StringBuilder("[");
	  int i=0;
	  for(IJavaTypeFormal f : getFormals()) {
		  sb.append(f).append(" = ").append(actuals.get(i));
		  i++;
	  }
	  return sb.toString();
  }
  
  @Override
  protected Iterable<? extends IJavaTypeFormal> getFormals() {
	  return getFormals(methodDecl);
  }
  
  private static Iterable<? extends IJavaTypeFormal> getFormals(IRNode md) {
	  List<IJavaTypeFormal> rv = new ArrayList<>();
	  for(IRNode formal : TypeFormals.getTypeIterator(SomeFunctionDeclaration.getTypes(md))) {
		  IJavaTypeFormal tf = JavaTypeFactory.getTypeFormal(formal); 
		  rv.add(tf);
	  }
	  return rv;
	  // TODO convert the code below to use this?
  }
  
  public static IJavaTypeSubstitution create(IBinder b, IRNode md, Map<IJavaType, IJavaType> map) {    
    if (map.isEmpty()) {
      return NULL;
    }
    List<IJavaType> actuals = new ArrayList<IJavaType>(map.size());
    for(IRNode formal : TypeFormals.getTypeIterator(SomeFunctionDeclaration.getTypes(md))) {
      IJavaTypeFormal tf = JavaTypeFactory.getTypeFormal(formal); 
      IJavaType actual   = map.get(tf);
      if (actual != null) {
        actuals.add(actual);
      } else {
        throw new IllegalArgumentException("no actual?");
      }
    }
    return new FunctionParameterSubstitution(b, md, actuals);
  }

  public static IJavaTypeSubstitution create(IBinder b, IBinding mbind, IJavaType[] targs) {
	  return new FunctionParameterSubstitution(b, mbind.getNode(), Arrays.asList(targs));
  }
  
  public static IJavaTypeSubstitution create(IBinder b, IBinding mbind, IRNode[] targs) {
	  List<IJavaType> actuals = new ArrayList<IJavaType>(targs.length);
	  for(IRNode actual : targs) {
	      IJavaType actualType  = b.getJavaType(actual);
	      actuals.add(actualType);
	  }
	  return new FunctionParameterSubstitution(b, mbind.getNode(), actuals);
  }
}
