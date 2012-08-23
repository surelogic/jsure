package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public final class AnnotationBoundVirtualDrop extends PromiseDrop<IAASTRootNode> {
  public AnnotationBoundVirtualDrop(
      final IRNode formalDecl, final String bound, final String formalName) {
    // We don't have an AST node
    super(null);
    setNodeAndCompilationUnitDependency(formalDecl);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setResultMessage(Messages.AnnotationBoundVirtual, bound, formalName);
    setVirtual(true);
  }
}
