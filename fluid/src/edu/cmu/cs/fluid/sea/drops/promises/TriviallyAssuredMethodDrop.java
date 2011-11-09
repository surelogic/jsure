package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;

/**
 * Promise drop for to be used as the top-level "promise" for
 * methods that are uninteresting from the perspective of uniqueness.
 * That is, the methods that are trivially assurable because they don't 
 * use any uniqueness features.
 */
public final class TriviallyAssuredMethodDrop extends PromiseDrop {
  private static final String LABEL = "Trivially assurable methods/constructors/intializers for ";
  
  private static final Map<IRNode,TriviallyAssuredMethodDrop> typeToDrop = 
    new HashMap<IRNode,TriviallyAssuredMethodDrop>(); 
  
  private TriviallyAssuredMethodDrop(final IRNode type) {
    this.setMessage(LABEL + JavaNames.getTypeName(type));
    this.setNodeAndCompilationUnitDependency(null);
    this.setCategory(JavaGlobals.UNIQUENESS_CAT);
  }
  
  public static synchronized TriviallyAssuredMethodDrop getDropFor(final IRNode n) {
    final IRNode type = VisitUtil.getEnclosingType(n);
    TriviallyAssuredMethodDrop drop = typeToDrop.get(type);
    if (drop == null) {
      drop = new TriviallyAssuredMethodDrop(type);
      drop.dependUponCompilationUnitOf(n);
      typeToDrop.put(type, drop);
    }
    return drop;
  }
}