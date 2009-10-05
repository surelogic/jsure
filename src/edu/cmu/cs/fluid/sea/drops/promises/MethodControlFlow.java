package edu.cmu.cs.fluid.sea.drops.promises;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Promise drop for to be used as the top-level "promise" for methods that are
 * uninteresting from the perspective of uniqueness. That is, the methods that
 * are trivially assurable because they don't use any uniqueness features.
 */
public final class MethodControlFlow extends PromiseDrop<IAASTRootNode> {

  private static final Map<IRNode, MethodControlFlow> blockToDrop = new HashMap<IRNode, MethodControlFlow>();

  private MethodControlFlow(final IRNode block) {
    final MessageFormat form = new MessageFormat(
        Messages.MethodControlFlow_otherControlDrop);
    final Object[] args = new Object[1];

    if (block != null) {
      final Operator op = JJNode.tree.getOperator(block);
      if (ConstructorDeclaration.prototype.includes(op)
          || MethodDeclaration.prototype.includes(op)) {
        args[0] = JavaNames.genMethodConstructorName(block);
      } else {
        args[0] = DebugUnparser.toString(block);
      }
    } else {
      args[0] = "[null]"; //$NON-NLS-1$
    }

    final String label = form.format(args);
    this.setMessage(label);
    this.setNodeAndCompilationUnitDependency(block);
    this.setCategory(JavaGlobals.UNIQUENESS_CAT);
  }

  public static synchronized MethodControlFlow getDropFor(final IRNode block) {
    MethodControlFlow drop = blockToDrop.get(block);
    if (drop == null || !drop.isValid()) {
      drop = new MethodControlFlow(block);
      drop.dependUponCompilationUnitOf(block);
      blockToDrop.put(block, drop);
    }
    return drop;
  }
}