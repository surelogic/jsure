package edu.cmu.cs.fluid.java.analysis;

/** The object identity equality predicate.
 */

import java.util.*;

import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.*;

public class EqPredicate extends Predicate {
  public static final SyntaxTreeInterface tree = JJNode.tree;

  private final IBinder binder;
  private final Effects effects;
  private final boolean equal;
  private final IRNode expr1, expr2;
  private final IRNode constructorContext;
  
  public EqPredicate(
      IBinder b, Effects e, boolean eq, IRNode e1, IRNode e2,
      final IRNode cc) {
    binder = b;
    effects = e;
    equal = eq;
    expr1 = e1;
    expr2 = e2;
    constructorContext = cc;
  }
  public static Predicate create(IBinder b, Effects e, boolean eq,
				 IRNode e1, IRNode e2, final IRNode cc) {
    return cache(new EqPredicate(b,e,eq,e1,e2, cc));
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof EqPredicate) {
      EqPredicate p = (EqPredicate)other;
      return equal == p.equal &&
	(expr1.equals(p.expr1) && expr2.equals(p.expr2) ||
	 expr1.equals(p.expr2) && expr2.equals(p.expr1));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return
      expr1.hashCode() +
      expr2.hashCode() +
      (equal ? 17 : 0);
  }

  @Override
  public String toString() {
    return DebugUnparser.toString(expr1) +
      (equal ? "==" : "!=") +
      DebugUnparser.toString(expr2);
  }

  @Override
  public Predicate canonicalize() {
    switch (compareExpressions(expr1,expr2)) {
    case -1:
      return
	equal ?
	TrivialPredicate.falsePredicate :
	TrivialPredicate.truePredicate;
    case 1:
      return
	equal ?
	TrivialPredicate.truePredicate :
	TrivialPredicate.falsePredicate;
    }
    return this;
  }

  @Override
  public int compare(Predicate other) {
    if (other instanceof TrivialPredicate) {
      return ((TrivialPredicate)other).isTrue() ?
	Logic.IMPLIES :
	Logic.IMPLIED;
    } else if (other instanceof EqPredicate) {
      EqPredicate p = (EqPredicate)other;
      int comp11 = compareExpressions(expr1,p.expr1);
      int comp22 = compareExpressions(expr2,p.expr2);
      if (comp11 != 0 && comp22 != 0) {
	/* if the comparisons are both equal, we are done */
	if (comp11 == 1 && comp22 == 1)
	  return equal == p.equal ? Logic.EQUAL : Logic.INVERSE;
	
	/* otherwise things are only interesting if at exactly one is true
	 */
	if (comp11 != comp22) {
	  if (equal && p.equal) return Logic.CONFLICT;
	  if (equal && !p.equal) return Logic.IMPLIES;
	  if (!equal && p.equal) return Logic.IMPLIED;
	  if (!equal && !p.equal) return Logic.COMPLEMENT;
	}
      }
      /* same thing now with cross comparison */
      int comp12 = compareExpressions(expr1,p.expr2);
      int comp21 = compareExpressions(expr2,p.expr1);
      if (comp12 != 0 && comp21 != 0) {
	if (comp12 == 1 && comp21 == 1)
	  return equal == p.equal ? Logic.EQUAL : Logic.INVERSE;
	if (comp12 != comp21) {
	  if (equal && p.equal) return Logic.CONFLICT;
	  if (equal && !p.equal) return Logic.IMPLIES;
	  if (!equal && p.equal) return Logic.IMPLIED;
	  if (!equal && !p.equal) return Logic.COMPLEMENT;
	}
      }
    }
    return Logic.INCOMPARABLE;
  }

  @Override
  public Iterator<Predicate> generate(Predicate other) {
    if (other instanceof EqPredicate) {
      EqPredicate p = (EqPredicate)other;
      if (equal || p.equal) {
        boolean andeq = equal & p.equal;
        if (compareExpressions(expr1,p.expr1) == 1)
          return new SingletonIterator<Predicate>(create(binder,effects,andeq,expr2,p.expr2,constructorContext));
        else if (compareExpressions(expr2,p.expr2) == 1)
          return new SingletonIterator<Predicate>(create(binder,effects,andeq,expr1,p.expr1,constructorContext));
        else if (compareExpressions(expr1,p.expr2) == 1)
          return new SingletonIterator<Predicate>(create(binder,effects,andeq,expr2,p.expr1,constructorContext));
        else if (compareExpressions(expr2,p.expr1) == 1)
          return new SingletonIterator<Predicate>(create(binder,effects,andeq,expr1,p.expr2,constructorContext));
      }
    }
    return EmptyIterator.prototype();
  }

  @Override
  public Set<Effect> effects() {
    // NB. expr1 and expr2 are from the same flow unit
    final IRNode flowUnit = IntraproceduralAnalysis.getFlowUnit(expr1, constructorContext);
    final Effects.Query query = effects.getEffectsQuery(flowUnit);
    final Set<Effect> result = new HashSet<Effect>();
    result.addAll(query.getResultFor(expr1));
    result.addAll(query.getResultFor(expr2));
    return Collections.unmodifiableSet( result );
  }

  /** Compare two expressions syntactically.
   * @return one of three values: <ul>
   * <li> 1: the expressions will definitely evaluate to the same value
   * <li> -1: the expressions will definitely evaluate to different things
   * <li> 0: unknown
   * </ul>
   */
  public int compareExpressions(IRNode n1, IRNode n2) {
    // This routine could be made very smart, but I'm going to
    // try to keep it simple. It doesn't handle x != x+1, for instance.
    Operator op = tree.getOperator(n1);
    if (tree.getOperator(n2) != op)
      return 0;
    if (op == IntLiteral.prototype) {
      String s1 = IntLiteral.getToken(n1);
      String s2 = IntLiteral.getToken(n2);
      // punt on octal or hexadecimal
      if (s1.charAt(0) == '0' || s2.charAt(0) == '0')
        return 0;
      return s1.equals(s2) ? 1 : -1;
    } else if (op == VariableUseExpression.prototype) {
      return binder.getBinding(n1).equals(binder.getBinding(n2)) ? 1 : 0;
    } else if (op == AddExpression.prototype || op == SubExpression.prototype) {
      final int subResult1 = compareExpressions(BinopExpression.getOp1(n1), BinopExpression.getOp1(n2));
      if (subResult1 == 0) {
        return 0;
      } else {
        final boolean equal = (subResult1 == 1);
        final int subResult2 = compareExpressions(BinopExpression.getOp2(n1), BinopExpression.getOp2(n2));
        if (subResult2 == 0) {
          return 0;
        } else if (subResult2 == -1) {
          return equal ? -1 : 0;
        } else if (subResult2 == 1) {
          return equal ? 1 : -1;
        }
      }
    } else if (BinopExpression.prototype.includes(op)) {
      return (compareExpressions(BinopExpression.getOp1(n1), BinopExpression
          .getOp1(n2)) == 1 && compareExpressions(BinopExpression.getOp2(n1),
          BinopExpression.getOp2(n2)) == 1) ? 1 : 0;
    } else if (UnopExpression.prototype.includes(op)) {
      return compareExpressions(UnopExpression.getOp(n1), UnopExpression
          .getOp(n2));
    }
    return 0;
  }
}
