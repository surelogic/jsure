/* $Header: /cvs/fluid/fluid/src/com/surelogic/analysis/bca/BindingContext.java,v 1.31 2008/05/15 16:24:11 aarong Exp $ */
package com.surelogic.analysis.bca;

import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidException;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/** Information about binding of locals to initial values,
 * newly dynamically allocated (unique) values, and unique fields.
 * Essentially this data structure is a pure table, indexed
 * by a fixed set of locals for a method.  The values for each
 * local is a set of objects it could be bound to.  The null information
 * case is when the set is infinite; the perfect information
 * is when the set is empty.  We try to avoid infinite sets;
 * if an expression computes an unknown reference, we put the expression
 * in the set, rather than return an infinite set.
 * The elements of the set are <ul>
 * <li> parameter declarations, which represent their initial values.
 * <li> receiver declarations, which represent the 'this' object.
 * <li> constructor or method calls that return unique objects,
 * which represent any instance of its evaluation during the execution
 * of the method.
 * <li> accesses of unique fields which represents any evaluation
 * of the given FieldRef expression IRNode.
 * <li> expressions returning unknown values.
 * </ul>
 * The set can be used to determine all the parameters or new
 * constructor calls an expression could evaluate to.
 * 
 * <em>All locals that are unknown (not in the lattice) are 
 * assumed to be of non-object type and has no objects.</em>
 */
public class BindingContext extends ArrayLattice<IRNode> {
  IRNode methodDecl;
  final IRNode[] locals;
  final IBinder binder;
  
  /** Create a new BindingContext lattice for a particular method. */
  public BindingContext(IRNode md, IRNode[] locals, IBinder binder) {
    super(new UnionLattice<IRNode>(),locals.length);
    this.methodDecl = md;
    this.locals = locals;
    this.binder = binder;
  }

  protected BindingContext(IRNode md, IRNode[] locals, IBinder binder,
			   Lattice<IRNode>[] values,
			   RecordLattice<IRNode> top, RecordLattice<IRNode> bottom) {
    super(values,top,bottom);
    this.methodDecl = md;
    this.locals = locals;
    this.binder = binder;
  }

  @Override
  protected RecordLattice<IRNode> newLattice(Lattice<IRNode>[] newValues) {
    return new BindingContext(methodDecl,locals,binder,newValues,top,bottom);
  }
    
  public BindingContext replaceValue(IRNode decl, ImmutableHashOrderSet<IRNode> objects) {
    try {
      int i = findLocalH(decl);
      if (i == -1) {
        return this;
      }
      UnionLattice<IRNode> oldSet = (UnionLattice<IRNode>)values[i];
      UnionLattice<IRNode> newSet = (UnionLattice<IRNode>)oldSet.cacheSet(objects);
      return (BindingContext)super.replaceValue(i,newSet);
    } catch (LocalNotFoundException e) {
      // for now:
//      System.out.println("Could not find local");
      return this;
    }
  }

  @SuppressWarnings("unchecked")
  public ImmutableHashOrderSet<IRNode> localObjects(IRNode local) {
    try {
      final int idx = findLocalH(local);
      if (idx == -1) {
        return CachedSet.getEmpty();
      }
      else return (ImmutableHashOrderSet<IRNode>)values[idx];
    } catch (LocalNotFoundException e) {
      return CachedSet.getUniverse();
    }
  }
  
  /** Return a set of objects that this expression could evaluate too.
   * An infinite set is used when the set may include unknown objects.
   * The binding context <tt>this</tt> is used to evaluate local variables.
   * <p> This code relies on the fact that in Java,
   * it is not possible to execute a side-effect reference assignment
   * after computing a reference value to return.
   */
  public ImmutableHashOrderSet<IRNode> expressionObjects(IRNode expr) {
    final Operator op = JJNode.tree.getOperator(expr);
    
    if (Initialization.prototype.includes(op)) {
      return expressionObjects(Initialization.getValue(expr));
    } else if (VariableUseExpression.prototype.includes(op)) {
      IRNode decl = binder.getBinding(expr);
      if (decl == null) return CachedSet.getEmpty();
      return localObjects(decl);
    } else if (ConditionalExpression.prototype.includes(op)) {
      ImmutableHashOrderSet<IRNode> s1 = expressionObjects(ConditionalExpression.getIftrue(expr));
      ImmutableHashOrderSet<IRNode> s2 = expressionObjects(ConditionalExpression.getIffalse(expr));
      return s1.union(s2);
    } else if (CastExpression.prototype.includes(op)) {
      return expressionObjects(CastExpression.getExpr(expr));
    } else if (NullLiteral.prototype.includes(op)) {
      return CachedSet.getEmpty();
    } else if (FieldRef.prototype.includes(op)) {
      /* XXX: John says "I find this dubious, but currently it doesn't do
       * anything because even if the field is NOT unique, the same thing is
       * returned."
       */
      IRNode fdecl = binder.getBinding(expr);
      if (fdecl != null && UniquenessRules.isUnique(fdecl)) {
	      return CachedSet.<IRNode>getEmpty().addElement(expr);
      }
    } else if (AssignExpression.prototype.includes(op)) {
      return expressionObjects(AssignExpression.getOp2(expr));
    } else if (MethodCall.prototype.includes(op)) {
      /* XXX: John says "I find this dubious, but currently it doesn't do
       * anything because even if the field is NOT unique, the same thing is
       * returned."
       */
      IRNode mdecl = binder.getBinding(expr);
      if (mdecl != null &&       
          UniquenessRules.isUnique(JavaPromise.getReturnNode(mdecl))) {
	      return CachedSet.<IRNode>getEmpty().addElement(expr);
      }
    } else if (ThisExpression.prototype.includes(op)) {
      final IRNode rec = JavaPromise.getReceiverNode(methodDecl);
      return CachedSet.<IRNode>getEmpty().addElement(rec);
    } else if (SuperExpression.prototype.includes(op)) {
      final IRNode rec = JavaPromise.getReceiverNode(methodDecl);
      return CachedSet.<IRNode>getEmpty().addElement(rec);
    } else if (QualifiedThisExpression.prototype.includes(op)) {
      final IRNode qualifiedRec =
        JavaPromise.getQualifiedReceiverNodeByName(methodDecl,
            binder.getBinding(QualifiedThisExpression.getType(expr)));
      return CachedSet.<IRNode>getEmpty().addElement(qualifiedRec);
    }

    /* By returning the expression rather than returning an infinite
     * set, this analysis can be used to track values within a method
     * (unique and limited, for instance).  In essence, the analysis
     * can be used in the place of reaching definitions analysis.
     */
    return CachedSet.<IRNode>getEmpty().addElement(expr);
    // or -- return CachedSet.getUniverse();
  }

  int findLocal(IRNode local) throws LocalNotFoundException {
    final int idx = findLocalH(local);
    if (idx == -1) throw new LocalNotFoundException();
    return idx;
  }

  int findLocalH(IRNode local) throws LocalNotFoundException {
    for (int i = 0; i < locals.length; ++i) {
      if (locals[i].equals(local)) return i;
    }
    return -1;
  }

  public static String elemToString(Object o) {
    if (o instanceof IRNode) {
      return DebugUnparser.toString((IRNode)o);
    } else {
      return o.toString();
    }
  }

  public static String setToString(ImmutableHashOrderSet s) {
    StringBuilder sb = new StringBuilder();
    if (s.isInfinite()) {
      sb.append("~");
      s = s.invert();
    }
    sb.append('{');
    int n = s.size();
    for (int i=0; i < n; ++i) {
      if (i != 0) sb.append(',');
      try {
	sb.append(elemToString(s.elementAt(i)));
      } catch (SetException e) {
	throw new FluidError("Impossible!");
      }
    }
    sb.append('}');
    sb.append(" (of ");
    sb.append(((AbstractCachedSet)s).cacheSize());
    sb.append(')');
    return sb.toString();
  }

  public static String localToString(Object l) {
    if (l instanceof IRNode) {
      IRNode n = (IRNode) l;
      Operator op = JJNode.tree.getOperator((IRNode) l);
      if (op instanceof VariableDeclarator) {
        return VariableDeclarator.getId(n);
      } else if (op instanceof ParameterDeclaration) {
        return ParameterDeclaration.getId(n);
      } else if (op instanceof ReceiverDeclaration) {
        return "this";
      } else if (op instanceof ReturnValueDeclaration) {
        return "return";
      }
    }
    return l.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i < locals.length; ++i) {
      sb.append("  ").append(localToString(locals[i])).append(" in ")
	.append(setToString((ImmutableHashOrderSet)getValue(i)))
	.append("\n");
    }
    return sb.toString();
  }
}

class LocalNotFoundException extends FluidException {
  LocalNotFoundException() { super(); }
  LocalNotFoundException(String s) { super(s); }
}
