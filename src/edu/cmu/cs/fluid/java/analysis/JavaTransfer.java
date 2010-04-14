/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/JavaTransfer.java,v 1.29
 * 2003/09/15 21:05:16 chance Exp $
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.control.CallInitializerLabel;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.Lattice;

/**
 * This abstract class contains routines useful for both forward and backward
 * control-flow analysis of Java programs. It implements some of the generic
 * transfer routines in terms of Java-specific transfer routines.
 * 
 * @see ForwardTransfer
 * @see BackwardTransfer
 * @see JavaForwardTransfer
 * @see JavaBackwardTransfer
 */
public abstract class JavaTransfer<T,V> {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("FLUID.control.java");

  protected SyntaxTreeInterface tree = JJNode.tree;
  protected final IntraproceduralAnalysis<T,V> baseAnalysis;
  protected final IBinder binder;

  public JavaTransfer(IntraproceduralAnalysis<T,V> base, IBinder b) {
    baseAnalysis = base;
    binder = b;
  }

  public Lattice<T> transferComponentFlow(
    IRNode node,
    Object info,
    Lattice<T> value) {
    Operator op = tree.getOperator(node);
    if (VariableUseExpression.prototype.includes(op)
      || FieldRef.prototype.includes(op)
      || ArrayLength.prototype.includes(op)
      || ArrayRefExpression.prototype.includes(op)
      || ConstructionObject.prototype.includes(op)
      || QualifiedSuperExpression.prototype.includes(op)
      || QualifiedThisExpression.prototype.includes(op)) {
      IRNode parent = tree.getParent(node);
      if (parent != null
        && AssignExpression.prototype.includes(tree.getOperator(parent))
        && node == AssignExpression.getOp1(parent)) {
        // skip:
        return value;
      } else {
        return transferUse(node, op, value);
      }
    } else if (VariableDeclarator.prototype.includes(op)) {
      return transferInitialization(node, value);
    } else if (ArrayInitializer.prototype.includes(op) && info == null) {
      /* allocate an array unless the child of an ArrayCreationExpression */
      IRNode parent = tree.getParent(node);
      if (parent != null
        && ArrayCreationExpression.prototype.includes(tree.getOperator(parent)))
        return value;
      return transferImplicitArrayCreation(node, value);
    } else if (SynchronizedStatement.prototype.includes(op)) {
      return transferMonitorAction(
        node,
        ((Boolean) info).booleanValue(),
        value);
    } else if (MethodBody.prototype.includes(op)) {
      return transferMethodBody(node, (Port) info, value);
    } else if (info == null && op instanceof CrementExpression) {
      return transferAssignment(node, value);
    } else {
      return transferOperation(node, op, info, value);
    }
  }

  public Lattice<T> transferComponentChoice(
    IRNode node,
    Object info,
    boolean flag,
    Lattice<T> value) {
    /* special cases for each kind of component choice node */
    Operator op = tree.getOperator(node);
    if (info instanceof CallInterface) {
      return transferCall(node, flag, value);
    } else if (op instanceof AssignmentInterface && info == null) {
      if (flag) {
        /* completed assignment */
        return transferAssignment(node, value);
      } else {
        /* exception because of array store error */
        IRNode lhs = ((AssignmentInterface) op).getTarget(node);
        // ignore possibility of intervening nodes
        // (such as parenthesized expressions):
        if (ArrayRefExpression.prototype.includes(tree.getOperator(lhs))) {
          return transferFailedArrayStore(node, value);
        } else {
          return null;
        }
      }
    } else if (FieldRef.prototype.includes(op)) {
      IRNode object = FieldRef.getObject(node);
      Operator oop = tree.getOperator(object);
      if (oop == SuperExpression.prototype
        || oop == ThisExpression.prototype
        || oop == QualifiedThisExpression.prototype
        || oop == TypeExpression.prototype
        || (binder != null &&  isStaticUse(node)))
        return flag ? value : null;
      return transferIsObject(object, flag, value);
    } else if (ArrayLength.prototype.includes(op)) {
      // SAME as above
      IRNode object = ArrayLength.getObject(node);
      Operator oop = tree.getOperator(object);
      if (oop == SuperExpression.prototype
        || oop == ThisExpression.prototype
        || oop == QualifiedThisExpression.prototype
        || oop == TypeExpression.prototype)
        return flag ? value : null;
      return transferIsObject(object, flag, value);
    } else if (UnboxExpression.prototype.includes(op)) {
      IRNode object = UnboxExpression.getOp(node);
      return transferIsObject(object, flag, value);
    } else if (MethodCall.prototype.includes(op)) {
      MethodCall call = (MethodCall) op;
      IRNode object = call.get_Object(node);
      Operator oop = tree.getOperator(object);
      if (oop == SuperExpression.prototype
        || oop == ThisExpression.prototype
        || oop == QualifiedThisExpression.prototype
        || oop == TypeExpression.prototype
        || isStaticUse(node))
        return flag ? value : null;
      return transferIsObject(object, flag, value);
    } else if (OuterObjectSpecifier.prototype.includes(op)) {
      IRNode object = OuterObjectSpecifier.getObject(node);
      Operator oop = tree.getOperator(object);
      if (oop == SuperExpression.prototype
	  || oop == ThisExpression.prototype
    || oop == QualifiedThisExpression.prototype
	  || oop == TypeExpression.prototype)
        return flag ? value : null;
      return transferIsObject(object, flag, value);
    } else if (
      RelopExpression.prototype.includes(op)
        || InstanceOfExpression.prototype.includes(op)) {
      return transferRelop(node, op, flag, value);
    } else if (BooleanLiteral.prototype.includes(op)) {
      if (flag == TrueExpression.prototype.includes(op)) {
        // this control is happening:
        return transferOperation(node, op, info, value);
      } else {
        return null; // no control goes through
      }
    } else if (ArrayRefExpression.prototype.includes(op)) {
      if ("array".equals(info)) {
        IRNode array = ArrayRefExpression.getArray(node);
        return transferIsObject(array, flag, value);
      } else {
        return transferBoundsCheck(node, flag, value);
      }
    } else if (SynchronizedStatement.prototype.includes(op)) {
      return transferIsObject(SynchronizedStatement.getLock(node), flag, value);
    } else if (ArrayCreationExpression.prototype.includes(op)) {
      IRNode dimExprs = ArrayCreationExpression.getAllocated(node);
      if (tree.numChildren(dimExprs) == 0)
        return flag ? transferArrayCreation(node, value) : null;
      return transferDimsCheck(dimExprs, flag, value);
    } else if (CastExpression.prototype.includes(op)) {
      return transferCastExpression(node, flag, value);
    } else if (SwitchBlock.prototype.includes(op)) {
      IRNode elem = tree.getChild(node,(IRLocation)info);
      IRNode label = SwitchElement.getLabel(elem);
      // we have to examine the label for a match
      return transferCaseMatch(label, flag, value);
    } else if (OpAssignExpression.prototype.includes(op)) {
      JavaOperator op2 = OpAssignExpression.getOp(node);
      if (DivRemExpression.prototype.includes(op2)) {
        return transferDivide(
          OpAssignExpression.getOp2(node),
          op2,
          flag,
          value);
      } else {
        // no exception will be raised:
        return flag ? transferOperation(node, op2, info, value) : null;
      }
    } else if (DivRemExpression.prototype.includes(op)) {
      return transferDivide(DivRemExpression.getDivisor(node), op, flag, value);
    } else if (info instanceof SuperExpression) {
      return transferCallClassInitializer(node, flag, value);
    } else if (op instanceof AssertStatement || op instanceof AssertMessageStatement) {
      // we don't know whether assertions are enabbled or not.
      return value;
    } else {
      if (lastOp != op || info != lastInfo) {
        LOG.warning("Unknown ComponentChoice branch: op = " + op + " info = " + info);
        lastOp = op;
        lastInfo = info;
      }
      return value;
    }
  }

  static Operator lastOp = null;
  static Object lastInfo = null;
  
  /** Return true if a static method or field reference. */
  protected final boolean isStaticUse(IRNode ref) {
    IRNode binding = binder.getBinding(ref);
    if (binding == null) {
      return false;
    } else if (JavaNode.getModifier(binding, JavaNode.STATIC)) {
      return true;
    } else if (tree.getOperator(binding) instanceof VariableDeclarator) {
      if (JavaNode
        .getModifier(
          tree.getParent(tree.getParent(binding)),
          JavaNode.STATIC)) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
	 * Transfer over an operation of some sort that combines values on the
	 * evaluation stack and may or may not produce a new value. This method is
	 * overridden in the class for abstract interpretations.
	 * 
	 * @see JavaEvaluationTransfer
	 */
  protected Lattice<T> transferOperation(
    IRNode node,
    Operator op,
    Object info,
    Lattice<T> value) {
    // by default return the same value
    return value;
  }

  /** Transfer over an operation involving a relational operator. */
  protected Lattice<T> transferRelop(
    IRNode node,
    Operator op,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value
    return value;
  }

  /**
	 * Transfer a lattice value over a method or constructor call.
	 * 
	 * @param call
	 *          the tree node containing the call
	 * @param flag
	 *          true for normal termination, false for abrupt termination
	 */
  protected Lattice<T> transferCall(IRNode call, boolean flag, Lattice<T> value) {
    return value;
  }

  /**
	 * Transfer a lattice value over the class initializer implied by the call.
	 * This is a NOP except for AnonClassExpression or a super constructor call.
	 * 
	 * @param call
	 *          the tree node containing the call
	 * @param flag
	 *          true for normal termination, false for abrupt termination
	 */
  protected final Lattice<T> transferCallClassInitializer(
    IRNode call,
    boolean flag,
    Lattice<T> value) {
    Operator op = tree.getOperator(call);
    if (op instanceof ConstructorCall
      && tree.getOperator(ConstructorCall.getObject(call))
        instanceof SuperExpression) {
      IRNode p = call;
      while (p != null && !(tree.getOperator(p) instanceof ClassBody)) {
        p = tree.getParentOrNull(p);
      }
      if (p != null) {
        return runClassInitializer(call, p, value, flag);
      }
    } else if (op instanceof AnonClassExpression) {
      return runClassInitializer(
        call,
        AnonClassExpression.getBody(call),
        value,
        flag);
    } else if (!flag) return null;
    return value;
  }

  /**
	 * Transfer a lattice value over a use of a local, parameter, receiver,
	 * field, or array element. This is <em>not</em> called in lvalue
	 * situations.
	 * 
	 * @param node
	 *          the use being examined.
	 */
  protected Lattice<T> transferUse(IRNode node, Operator op, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over an assignment.
	 * 
	 * @param node
	 *          the assignment node.
	 */
  protected Lattice<T> transferAssignment(IRNode node, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over an initialization of a field or local
	 * 
	 * @param node
	 *          the declarator node.
	 */
  protected Lattice<T> transferInitialization(IRNode node, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over a reference of an object, that is a test of
	 * whether it is instanceof Object (non-null).
	 * 
	 * @param node
	 *          the node containing the object expression NB: in the case we have
	 *          an array access, the object may have been modified in the index
	 *          expression following.
	 * @param flag
	 *          true if reference was successful (non-null), false otherwise.
	 */
  protected Lattice<T> transferIsObject(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over an array reference that is in bounds.
	 * 
	 * @param node
	 *          the array reference node
	 * @param flag
	 *          true if in bounds, false if out of bounds
	 */
  protected Lattice<T> transferBoundsCheck(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over a parenthesized expression
	 * 
	 * @param node
	 *          the parenthesized node
	 */
  protected Lattice<T> transferParens(IRNode node, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over an assignment into an array that fails
	 * because of ArrayStoreException.
	 * 
	 * @param node
	 *          the assignment node.
	 */
  protected Lattice<T> transferFailedArrayStore(IRNode node, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over a constant case label in a switch statement.
	 * 
	 * @param label
	 *          the SwitchLabel node
	 * @param flag
	 *          whether the case that a match is successful
	 */
  protected Lattice<T> transferCaseMatch(
    IRNode label,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a value over a divide or remainder operation.
	 * 
	 * @param divisor
	 *          the node containing the divisor expression.
	 * @param flag
	 *          successful divide (divisor != 0)
	 */
  protected Lattice<T> transferDivide(
    IRNode divisor,
    Operator op,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a value over an array creation's dimensions. The true branch
	 * should handle anything regarding allocating the array
	 * 
	 * @param dimExprs
	 *          the node containing the dimension expressions
	 * @param flag
	 *          whether all indices are nonnegative.
	 */
  protected Lattice<T> transferDimsCheck(
    IRNode dimExprs,
    boolean flag,
    Lattice<T> value) {
    // by default look to see if we have some all nonnegative constants:
    Iterator<IRNode> ch = tree.children(dimExprs);
    while (ch.hasNext()) {
      IRNode child = ch.next();
      Operator op = tree.getOperator(child);
      /* if we can't tell, we just return the value */
      if (op != IntLiteral.prototype)
        return value;
      /* if we have a negative literal, we know we have problems */
      if ((IntLiteral.getToken(child)).charAt(0) == '-')
        return flag ? null : value;
    }
    /* all are positive integer literals */
    return flag ? value : null;
  }

  /**
	 * Transfer a value over an implicit array creation around an array
	 * initializer.
	 * 
	 * @param arrayInitializer
	 *          the node containing the initializer
	 * @param value
	 *          old lattice value.
	 */
  protected Lattice<T> transferImplicitArrayCreation(
    IRNode arrayInitializer,
    Lattice<T> value) {
    return transferArrayCreation(arrayInitializer, value);
  }

  /**
	 * Transfer evaluation over an array creation. <strong>major grouping, leaf
	 * </strong>
	 * 
	 * @param node
	 *          the dimensions or the initializer
	 */
  protected Lattice<T> transferArrayCreation(IRNode node, Lattice<T> value) {
    return value;
  }

  /**
	 * Transfer a value over a cast expression.
	 * 
	 * @param node
	 *          the cast expression node
	 * @param flag
	 *          whether cast was successful
	 */
  protected Lattice<T> transferCastExpression(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /** Transfer lattice value over source/sink for a method body. */
  protected Lattice<T> transferMethodBody(IRNode node, Port kind, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer lattice value over a synchronized statement enter or exit.
	 * 
	 * @param node
	 *          the synchronized statement
	 * @param enter
	 *          true for entering, false for exiting
	 */
  protected Lattice<T> transferMonitorAction(
    IRNode node,
    boolean enter,
    Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Perform the instance initializer and return the result after termination.
	 * 
	 * @param caller
	 *          the node to be associated with the call.
	 * @param classBody
	 *          the node for the class body.
	 * @param initial
	 *          the initial lattice value
	 * @param terminationNormal
	 *          if true then return result of normal termination, otherwise
	 *          result of abrupt termination.
	 */
  protected final Lattice<T> runClassInitializer(
    IRNode caller,
    IRNode classBody,
    Lattice<T> initial,
    boolean terminationNormal) {
    /*
     * What we do here is problematic, but it is an attempt to 
     * work with "lattice poisoning" analyses.  We make a new
     * analysis (see below) and run it on the class instance
     * initializer.  The resulting lattice value is fed back into
     * this analysis.  In other words, we temporarily pause this analysis
     * and run another.  This could be inefficient but the class
     * initializer should be run only once, since it rarely occurs in a loop,
     * and because it mainly consists of straight-line code.  Semantically,
     * the class initializer is inlined into the constructor body.
     * 
     * The problem is how to handle lattice-poisoning analysis:
     * if a problem happens during the class initializer, because
     * of something that happened in the constructor, the lattice will be
     * poisoned but nothing in the constructor will be held at fault.
     * Furthermore, it could be when the class initializer is analyzed on its own, no
     * problem is found.  Thus UniquenessAssurance won't find where the problem
     * happened.  This is probably not a problem for a side-effecting analysis
     * because the problem will still be identified.
     * 
     * Thus in order to help out lattice-poisoning analyses, we don't
     * actually get a *new* analysis.  Instead we get a cached analysis,
     * but then initialize it with a unique label so that each inlining of
     * the class initializer gets its own analysis results.  Any problems
     * found however will be found for all labels (because of how UniquenessAssurance
     * works) and so will be reported, provided that the same cached analysis
     * is found.  This design decision rests on two shaky assumptions:
     * 1> that the cache won't jettison the analysis for the class initializer
     *    before UniquenessAssurance finds it.  Putting things in a cache
     *    that can't be recomputed is a bad idea.
     * 2> that it is OK to restart an analysis that is already finished:
     *    assuming the cache works as hoped, we will be reinitializing
     *    an analysis that has already completed.  Some new flow analysis
     *    implementation possibilities won't necessarily want to handle restarting
     *    an analysis, and the semantics are dubious at best.
     * For these reasons we are looking to change the design to run a fresh
     * (uncached) analysis each time.  This would make lattice-poisoning analyses
     * less useful.
     */
    if (baseAnalysis == null)
      return initial; // no way of determining what the analysis to run is
    FlowUnit op = (FlowUnit) tree.getOperator(classBody);
    FlowAnalysis<T> fa = baseAnalysis.getAnalysis(classBody); // TODO: change to createAnalysis
    Source source = op.getSource(classBody);
    ControlLabel cil = new CallInitializerLabel(caller);
    LabelList ll = LabelList.empty.addLabel(cil);
    fa.initialize(source.getOutput(), ll, initial);
    fa.performAnalysis();
    Sink sink;
    if (terminationNormal) {
      sink = op.getNormalSink(classBody);
    } else {
      sink = op.getAbruptSink(classBody);
    }
    return fa.getInfo(sink.getInput(), ll);
  }
}
