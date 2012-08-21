package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public final class UniquenessControlFlowDrop extends PromiseDrop<IAASTRootNode> {
  /**
   * Construct a new control flow drop that represents the control flow for
   * the given method/constructor declaration.
   */
  public UniquenessControlFlowDrop(final IRNode mdecl) {
    // We don't have an AST node
    super(null);
    setNodeAndCompilationUnitDependency(mdecl);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
    setResultMessage(Messages.ControlFlow, JavaNames.genMethodConstructorName(mdecl));
    setVirtual(true);
  }
}
