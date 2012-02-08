package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import com.surelogic.analysis.alias.IMayAlias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

final class Add implements Apply {
  private final IMayAlias mayAlias;
  private final IRNode decl;
  private final Object var;
  private final ImmutableHashOrderSet<Object> additional;

  /**
   * Add the set of nodes wherever the target variable v exists.
   * @param v variable to look for
   * @param add set to add to objects that have v in them.
   */
  public Add(final Object v, final ImmutableHashOrderSet<Object> add) {
	  this(v,add,null,null);
  }
  
  /**
   * Add the set of nodes wherever the target variable v exists,
   * and the elements of the set could possibly alias the expression/declaration node.
   * (Principally, this means we compare the types.)
   * @param v variable to look for
   * @param add set of add to objects that have v in them
   * @param d expression/declaration node to see if aliasing possible
   * @param ma alias analysis to use
   */
  public Add(final Object v, final ImmutableHashOrderSet<Object> add, 
      final IRNode d, final IMayAlias ma) {
    mayAlias = ma;
    decl = d;
    var = v;
    additional = add;
  }

  public ImmutableHashOrderSet<Object> apply(
		  final ImmutableHashOrderSet<Object> other) {
	  if (other.contains(var)) {
		  /* Only add if ALL the receiver/parameter declarations MAY ALIAS
		   * the node decl.  If there aren't any in the set, we add additional too. 
		   */
		  if (decl != null && mayAlias != null) {
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
		  }
		  return other.union(additional);
	  }
    return other;
  }
}
