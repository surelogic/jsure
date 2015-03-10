package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

import com.surelogic.analysis.alias.IMayAlias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

final class AddBetter implements Apply {
  private final IMayAlias mayAlias;
  private final IRNode decl;
  
  private final Object var;
  private final ImmutableHashOrderSet<Object> additional;

  public AddBetter(final IMayAlias ma, final IRNode d, 
      final Object v, final ImmutableHashOrderSet<Object> add) {
    mayAlias = ma;
    decl = d;
    
    var = v;
    additional = add;
  }

  @Override
  public ImmutableHashOrderSet<Object> apply(
      final ImmutableHashOrderSet<Object> other) {
    if (other.contains(var)) {
      /* Only add if ALL the receiver/parameter declarations MAY ALIAS
       * the node decl.  If there aren't any in the set, we add additional too. 
       */
      for (final Object o : other) {
        if (o instanceof IRNode) {
          final IRNode n = (IRNode) o;
          final Operator op = JJNode.tree.getOperator(n);
          if (ReceiverDeclaration.prototype.includes(op) ||
              QualifiedReceiverDeclaration.prototype.includes(op) ||
              ParameterDeclaration.prototype.includes(op)) {
            if (!mayAlias.mayAlias(decl, n)) {
              // Cannot be an alias, abort
              return other;
            }
          }
        }
      }
      return other.union(additional);
    }
    return other;
  }
}
