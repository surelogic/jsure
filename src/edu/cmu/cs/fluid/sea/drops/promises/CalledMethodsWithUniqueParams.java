package edu.cmu.cs.fluid.sea.drops.promises;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for to be used as the top-level "promise" for
 * methods that are uninteresting from the perspective of uniqueness.
 * That is, the methods that are trivially assurable because they don't 
 * use any uniqueness features.
 */
public final class CalledMethodsWithUniqueParams extends PromiseDrop<IAASTRootNode> {
  private static final String LABEL_TEMPLATE = "Call sites in {0} of methods with unique parameters";
  
  private static final Map<IRNode,CalledMethodsWithUniqueParams> typeToDrop = 
    new HashMap<IRNode,CalledMethodsWithUniqueParams>(); 
  
  private CalledMethodsWithUniqueParams(final IRNode type) {
    final MessageFormat form = new MessageFormat(LABEL_TEMPLATE);
    final String label = form.format(new Object[] { JavaNames.getTypeName(type) });
    this.setMessage(label);
    this.setNodeAndCompilationUnitDependency(null);
    this.setCategory(JavaGlobals.UNIQUENESS_CAT);
  }
  
  public static synchronized CalledMethodsWithUniqueParams getDropFor(final IRNode n) {
    final IRNode type = VisitUtil.getEnclosingType(n);
    CalledMethodsWithUniqueParams drop = typeToDrop.get(type);
    if (drop == null) {
      drop = new CalledMethodsWithUniqueParams(type);
      drop.dependUponCompilationUnitOf(n);
      typeToDrop.put(type, drop);
    }
    return drop;
  }
}