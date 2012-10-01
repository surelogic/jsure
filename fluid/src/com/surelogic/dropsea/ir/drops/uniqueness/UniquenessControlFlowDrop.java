package com.surelogic.dropsea.ir.drops.uniqueness;

import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Currently taking a dummy UniqueNode
 */
public final class UniquenessControlFlowDrop extends PromiseDrop<UniqueNode> {
  /**
   * Construct a new control flow drop that represents the control flow for
   * the given method/constructor declaration.
   */
  private UniquenessControlFlowDrop(final UniqueNode n) {
    super(n);
  }
  
  public static UniquenessControlFlowDrop create(final IRNode mdecl) {
	  // Created just for the mdecl
	  UniqueNode dummy = new UniqueNode(-1, false);
	  dummy.setPromisedFor(mdecl);
	  
	  UniquenessControlFlowDrop result = new UniquenessControlFlowDrop(dummy);
	  result.setCategorizingMessage(JavaGlobals.UNIQUENESS_CAT);
	  result.setMessage(Messages.ControlFlow, XUtil.useExperimental() ? JavaNames.genRelativeFunctionName(mdecl) :
		  JavaNames.genMethodConstructorName(mdecl));
	  result.setVirtual(true);
	  return result;
  }
}
