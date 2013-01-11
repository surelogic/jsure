package edu.uwm.cs.fluid.java.control;

import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.uwm.cs.fluid.control.*;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.control.UnknownLabel;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.uwm.cs.fluid.util.Lattice;

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
public abstract class JavaTransfer<L extends Lattice<T>, T> {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("FLUID.control.java");

  private static final boolean TRACE = false;
  
  protected SyntaxTreeInterface tree = JJNode.tree;
  protected final IBinder binder;
  protected final L lattice;
  private final SubAnalysisFactory<L, T> subAnalysisFactory;
  
  
  public JavaTransfer(final IBinder b, final L l, SubAnalysisFactory<L, T> factory) {
    binder = b;
    lattice = l;
    subAnalysisFactory = factory;
  }
  
  /* Only to be used by JavaBackwardAnalysis and JavaForwardAnalysis */
  SubAnalysisFactory<L, T> getSubAnalysisFactory() {
    return subAnalysisFactory;
  }
  
  public final T transferComponentFlow(
    IRNode node,
    Object info,
    T value) {
    Operator op = tree.getOperator(node);

    if (TRACE) {
      System.out.println("===> transferComponentFlow: " + op.name() + " " + DebugUnparser.toString(node));
      System.out.println("       info = " + info);
    }
    
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
    } else if (TypeDeclarationStatement.prototype.includes(op)) {
      return transferNestedClass(TypeDeclarationStatement.getTypedec(node), value);
    } else if (EnumConstantDeclaration.prototype.includes(op)) {
      return transferEnumConstantDeclaration(node, value);
    } else {
      return transferOperation(node, op, info, value);
    }
  }

  public final T transferComponentChoice(
    IRNode node,
    Object info,
    boolean flag,
    T value) {
    /* special cases for each kind of component choice node */
    Operator op = tree.getOperator(node);
    
    if (TRACE) {
      System.out.println("===> transferComponentChoice: " + op.name() + " " + DebugUnparser.toString(node));
      System.out.println("       info = " + info);
      System.out.println("       flag = " + flag);
    }
    
    if (info instanceof CallInterface) {
      if (ImpliedEnumConstantInitialization.prototype.includes(node)) {
        return transferImpliedNewExpression(node, flag, value);
      } else {
        return transferCall(node, flag, value);
      }
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
    } else if (op instanceof Resource) {
      return transferResourceClose(node,flag,value);
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
  protected T transferOperation(
    IRNode node,
    Operator op,
    Object info,
    T value) {
    // by default return the same value
    return value;
  }

  /** Transfer over an operation involving a relational operator. */
  protected T transferRelop(
    IRNode node,
    Operator op,
    boolean flag,
    T value) {
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
  protected T transferCall(IRNode call, boolean flag, T value) {
    return value;
  }
  
  /**
   * Transfer a lattice value over the implied constructor call of an 
   * enumeration constant declaration.
   */
  protected T transferImpliedNewExpression(
      final IRNode impliedInit, final boolean flag, final T value) {
    // N.B. Should be handled as a specialized case of transferCall
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
  protected final T transferCallClassInitializer(
    IRNode call,
    boolean flag,
    T value) {
    // First handle the construction of the object
    value = transferConstructorCall(call, flag, value);
    
    // Then handle any field inits and init blocks
    Operator op = tree.getOperator(call);
    if (ConstructorCall.prototype.includes(op) 
        && SuperExpression.prototype.includes(tree.getOperator(ConstructorCall.getObject(call)))) {
      IRNode p = call;
      while (p != null && !(tree.getOperator(p) instanceof ClassBody)) {
        p = tree.getParentOrNull(p);
      }
      if (p != null) {
        return runClassInitializer(call, p, value, flag);
      }
    } else if (AnonClassExpression.prototype.includes(op)) {
      return runClassInitializer(
        call,
        AnonClassExpression.getBody(call),
        value,
        flag);
    } else if (ImpliedEnumConstantInitialization.prototype.includes(op)
        && EnumConstantClassDeclaration.prototype.includes(tree.getParent(call))) {
      return runClassInitializer(
          JJNode.tree.getParent(call),
          EnumConstantClassDeclaration.getBody(JJNode.tree.getParent(call)),
          value,
          flag);
    } // else if (!flag) return null;
    return value;
  }

  /**
   * Transfer a lattice value over the creation of a new instance of the 
   * class.  This is called from {@link #transferCallClassInitializer}
   * before the class initialization (fields and init blocks) are handled.
   */
  protected T transferConstructorCall(final IRNode call, final boolean flag, final T value) {
    return flag ? value : null;
  }
  
  /**
	 * Transfer a lattice value over a use of a local, parameter, receiver,
	 * field, or array element. This is <em>not</em> called in lvalue
	 * situations.
	 * 
	 * @param node
	 *          the use being examined.
	 */
  protected T transferUse(IRNode node, Operator op, T value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over an assignment.
	 * 
	 * @param node
	 *          the assignment node.
	 */
  protected T transferAssignment(IRNode node, T value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over an initialization of a field or local
	 * 
	 * @param node
	 *          the declarator node.
	 */
  protected T transferInitialization(IRNode node, T value) {
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
  protected T transferIsObject(
    IRNode node,
    boolean flag,
    T value) {
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
  protected T transferBoundsCheck(
    IRNode node,
    boolean flag,
    T value) {
    // by default return the same value:
    return value;
  }

  /**
	 * Transfer a lattice value over a parenthesized expression
	 * 
	 * @param node
	 *          the parenthesized node
	 */
  protected T transferParens(IRNode node, T value) {
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
  protected T transferFailedArrayStore(IRNode node, T value) {
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
  protected T transferCaseMatch(
    IRNode label,
    boolean flag,
    T value) {
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
  protected T transferDivide(
    IRNode divisor,
    Operator op,
    boolean flag,
    T value) {
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
  protected T transferDimsCheck(
    IRNode dimExprs,
    boolean flag,
    T value) {
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
  protected T transferImplicitArrayCreation(
    IRNode arrayInitializer,
    T value) {
    return transferArrayCreation(arrayInitializer, value);
  }

  /**
	 * Transfer evaluation over an array creation. <strong>major grouping, leaf
	 * </strong>
	 * 
	 * @param node
	 *          the dimensions or the initializer
	 */
  protected T transferArrayCreation(IRNode node, T value) {
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
  protected T transferCastExpression(
    IRNode node,
    boolean flag,
    T value) {
    // by default return the same value:
    return value;
  }

  /** Transfer lattice value over source/sink for a method body. */
  protected T transferMethodBody(IRNode node, Port kind, T value) {
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
  protected T transferMonitorAction(
    IRNode node,
    boolean enter,
    T value) {
    // by default return the same value:
    return value;
  }

  /**
   * Transfer over a nested class declaration inside of a method.
   * In other words, close over used variables.
   * @param node nested class declaration statement
   * @param value value to transfer over
   * @return new lattice value
   */
  protected T transferNestedClass(
		  IRNode node,
		  T value ) {
	  return value;
  }
  
  /**
   * Transfer over the declaration of an enumeration constant declaration.
   */
  protected T transferEnumConstantDeclaration(final IRNode node, final T value) {
    // N.B. Should be handled as a special case of field initialization
    return value;
  }
  
  /**
   * Transfer over a resource close operation.
   * This is a kind of call.  It also means the resource variable goes
   * out of scope.
   * @param node Resource node being closed
   * @param flag normal (true) or abrupt (false) termination of close method
   * @param value value to transfer over
   * @return new lattice value.
   */
  protected T transferResourceClose(
		  IRNode node,
		  boolean flag,
		  T value  ) {
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
  protected final T runClassInitializer(
    final IRNode caller, final IRNode classBody,
    final T initial, final boolean terminationNormal) {
    if (TRACE) {
      System.out.println("**** Run class Initializer " + terminationNormal + " (start) ****");
    }
    
    FlowUnit op = (FlowUnit) tree.getOperator(classBody);
    final IJavaFlowAnalysis<T, L> fa = subAnalysisFactory.createSubAnalysis(
        caller, binder, lattice, initial, terminationNormal);
    final JavaComponentFactory factory = JavaComponentFactory.startUse();
    try {
    final Source source = op.getSource(classBody, factory);
    final Sink sink = terminationNormal ? op.getNormalSink(classBody, factory) : op.getAbruptSink(classBody, factory);
    final ControlEdge e1 = getStartEdge(source, sink); 
    final ControlEdge e2 = getEndEdge(source, sink);
    
    /* John and I thought this was wrong back on 2010-01-12, and changed 
     * the label list to be "LabelList.empty.addLabel(UnknownLabel.prototype)",
     * but that was a mistake.  The actual error we were trying to fix at the
     * time was most likely caused by the below issue with the initial value.
     * Using the UNKNOWN label causes monotonicity problems with instance
     * initializers.  John never gave me a justification for why changing to the
     * unknown label made any sense any how.  On 2010-12-07 I changed this back
     * to use the empty list.  Later in Dec 2010, I realized that what we need
     * to do is use the Unknown label for the abrupt case and empty list for the
     * normal case.  I tried this change out, but it didn't seem to fix my then current
     * broken examples because of a problem with "dynamicSplitCombiner" in
     * BackwardAnalysis.  Because it didn't help, I didn't leave the change in.
     * Now that we have fixed BackwardAnalysis, I am correcting this again 
     * (on 2011-02-03).
     */
    final LabelList ll = terminationNormal ?
        LabelList.empty : LabelList.empty.addLabel(UnknownLabel.prototype);

    /* We used to just initialize with the 'initial' value.  Turns out this 
     * is problematic because the same subanalysis object is returned by 
     * createAnalysis() in most (currently all) cases.  We ran into a case
     * where the initial values for the normal and exceptional cases where
     * sufficiently different as to create a monotonicity failure:
     * (1) the normal case would proceed and the subanalysis would be initialized
     * for the first time with the initial value V1.
     * (2) the exceptional case would proceed and the subanalysis would be 
     * reinitialized with a different initial value v2.  This was okay because 
     * V1 was less than or equal to V2.
     * (3) The flow analysis would iterate again, and revisit the normal case,
     * reinitializing with V1.  This would trigger a monotonicity error because
     * V2 was not less than or equal to V1.
     * 
     * So now we join the 'initial' value with the existing value.
     */
    final T existingValue = fa.getInfo(e1, ll);
    final T joined;
    if (initial != null && existingValue != null) { 
      joined = lattice.join(initial, existingValue);
    } else {
      joined = initial;
    }
    
//    final String is = initial == null ? "null" : lattice.toString(initial);
//    final String es = existingValue == null ? "null" : lattice.toString(existingValue);
//    final String js = joined == null ? "null" : lattice.toString(joined);
    
    fa.initialize(e1, ll, joined);
    
    // I'm worried that the analysis may wish to call (say)
    // transferComponentSource and then wonder why bottom() isn't
    // the same as what we put here.  We may need to hook this little
    // CFG into the main CFG
    fa.performAnalysis();
    
    if (TRACE) {
      System.out.println("**** Run class Initializer " + terminationNormal + " (end) ****");
    }
    return fa.getInfo(e2, ll);
    } finally {
    	JavaComponentFactory.finishUse(factory);
    }
  }
    
  /**
   * Get the starting edge for the analysis of call initializers.  This is the
   * edge that will be used to initialize the analysis.
   */
  protected abstract ControlEdge getStartEdge(Source src, Sink sink);
  
  /**
   * Get the ending edge for the analysis of call initializers.  This is the
   * edge that will be queried for the analysis result.
   */
  protected abstract ControlEdge getEndEdge(Source src, Sink sink);
}
