package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/* Promise drop that bundles together all the results for instantiating a 
 * generic type that has annotation bounds.
 */
public final class TypeInstantiationDrop extends PromiseDrop<IAASTRootNode> {
  public TypeInstantiationDrop(final IRNode typeDecl) {
    // We don't have an AST node
    super(null);
    setNodeAndCompilationUnitDependency(typeDecl);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setResultMessage(Messages.InstantiatedGeneric, JavaNames.getFullTypeName(typeDecl));
    setVirtual(true);
  }
}
