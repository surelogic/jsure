/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/JavaEvaluationTransfer.java,v
 * 1.24 2003/09/15 21:05:16 chance Exp $
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.logging.Level;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Lattice;

/**
 * An analysis transfer function interested in the evaluation of functions, for
 * example, in an abstract interpretation. A stack is implied, and all the
 * transfer methods defined here keep the stack correctly in sync (and nothing
 * more). The push and pop stack methods are abstract and must be implemented
 * by any concrete subclasses.
 */
public abstract class JavaEvaluationTransfer<T,V> extends JavaForwardTransfer<T,V> {
  public JavaEvaluationTransfer(IntraproceduralAnalysis<T,V> ba, IBinder binder) {
    super(ba, binder);
  }

  // abstract stack operations.

  /** Pop an element from the stack and discard it. */
  protected abstract Lattice<T> pop(Lattice<T> val);
  /** Push an unknown element onto the stack. */
  protected abstract Lattice<T> push(Lattice<T> val);

  /**
	 * Pop all pending arguments from the stack because of an exception being
	 * raised.
	 */
  protected abstract Lattice<T> popAllPending(Lattice<T> val);

  /** Pop a specified number of elements off the stack. */
  protected Lattice<T> pop(Lattice<T> val, int n) {
    for (; n > 0; --n) {
      val = pop(val);
    }
    return val;
  }

  /** Pop the second from top element from stack */
  protected Lattice<T> popSecond(Lattice<T> val) {
    return push(pop(pop(val)));
  }

  /** Duplicate the top element on the stack */
  protected Lattice<T> dup(Lattice<T> val) {
    return push(val);
  }

  /**
	 * Perform a transfer over a ComponentFlow which operates on the stack of
	 * pending expression evaluations. This method performs case analysis and
	 * also handles a few cases directly which involve simply discarding a single
	 * pending value.
	 * <p>
	 * To customize this class for a specific abstract evaluation, it is
	 * necessary only to override all the major grouping methods or to override
	 * the leaf methods. Each method is tagged with whether it is a major
	 * grouping method or a leaf method, both or neither.
	 * </p>
	 */
  @Override
  protected final Lattice<T> transferOperation(
    IRNode node,
    Operator op,
    Object info,
    Lattice<T> val) {
    if (Initializer.prototype.includes(op)) {
      if (LiteralExpression.prototype.includes(op)) {
        return transferLiteral(node, val);
      } else if (PrimaryExpression.prototype.includes(op)) {
        if (info instanceof NewExpression) {
          /* allocation of new object */
          return transferAllocation(node, val);
        } else if (AnonClassExpression.prototype.includes(op)) {
          /* store finals used in body inside new object */
          return transferAnonClass(node, val);
        } else if (TypeExpression.prototype.includes(op)) {
          return transferType(node, val);
        }
      } else if (BinopExpression.prototype.includes(op)) {
        if (ConditionalAndExpression.prototype.includes(op)
          || ConditionalOrExpression.prototype.includes(op)) {
          /* discard value not causing short-circuit: */
          return pop(val);
        } else
          return transferBinop(node, op, val);
      } else if (UnopExpression.prototype.includes(op)) {
        return transferUnop(node, op, val);
      } else if (ArrayInitializer.prototype.includes(op)) {
        if (info == null) {
          /* possibly allocate memory */
          IRNode p = tree.getParentOrNull(node);
          if (p != null
            && !tree.getOperator(p).includes(ArrayCreationExpression.prototype)) {
            return transferImplicitArrayCreation(node, val);
          } else {
            return val;
          }
        } else {
          /* storing of individual elements */
          return transferArrayInitializer(node, val);
        }
      } else if (ConditionalExpression.prototype.includes(op)) {
        return pop(val); // discard value without consideration
      } else if (ClassExpression.prototype.includes(op)) {
        return transferClassExpression(node, val);
      } else if (ParenExpression.prototype.includes(op)) {
        return transferParens(node, val);
      }
    } else if (Statement.prototype.includes(op)) {
      if (BlockStatement.prototype.includes(op)) {
        return transferCloseScope(node,val);
      } else if (/* discard values without consideration */
        AssertMessageStatement.prototype.includes(op)
          || AssertStatement.prototype.includes(op)
          || DoStatement.prototype.includes(op)
          || ExprStatement.prototype.includes(op)
          || ForStatement.prototype.includes(op)
          || IfStatement.prototype.includes(op)
          // || IfElseStatement.prototype.includes(op)
          || WhileStatement.prototype.includes(op)) {
        return pop(val);
      } else if (ReturnStatement.prototype.includes(op)) {
        return transferReturn(node, val);
      } else if (SwitchStatement.prototype.includes(op)) {
        return transferSwitch(node, val);
      } else if (ThrowStatement.prototype.includes(op)) {
        return transferThrow(node, val);
      }
    } else if (NoInitialization.prototype.includes(op)) {
      return transferDefaultInit(node, val);
    } else if (StatementExpressionList.prototype.includes(op)) {
      /* discard each expression as it is evaluated */
      return pop(val);
    }
    throw new FluidError(
      "No transition defined for " + op + " with info=" + info);
  }

  /**
	 * Transfer a lattice value over addition (numeric only). <strong>leaf
	 * </strong>
	 */
  protected Lattice<T> transferAdd(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over allocation of new object. <strong>major
	 * grouping, leaf</strong>
	 */
  protected Lattice<T> transferAllocation(IRNode node, Lattice<T> value) {
    return push(value);
  }

  /**
	 * Transfer a lattice value over &amp; connective. <strong>leaf</strong>
	 */
  protected Lattice<T> transferAnd(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer evaluation over the storing of finals used in body of
	 * AnonClassExpression. <strong>leaf</leaf>
	 */
  protected Lattice<T> transferAnonClass(IRNode node, Lattice<T> value) {
    return value;
  }

  /**
	 * Transfer evaluation over an array creation. <strong>major grouping, leaf
	 * </strong>
	 * 
	 * @param node
	 *          the dimensions or the initializer
	 */
  @Override
  protected Lattice<T> transferArrayCreation(IRNode node, Lattice<T> val) {
    if (tree.getOperator(node) instanceof DimExprs) {
      val = pop(val, tree.numChildren(node));
    }
    return push(val);
  }

  /**
	 * Transfer evaluation over array element storing <strong>major grouping
	 * </strong>
	 */
  protected Lattice<T> transferArrayInitializer(IRNode node, Lattice<T> val) {
    return pop(val);
  }

  /**
	 * Transfer evaluation over an assignment (assuming no array store error).
	 * <strong>major grouping</strong>
	 */
  @Override
  protected Lattice<T> transferAssignment(IRNode node, Lattice<T> val) {
    IRNode lhs = ((AssignmentInterface) tree.getOperator(node)).getTarget(node);
    Operator lop = tree.getOperator(lhs);
    if (lop instanceof VariableUseExpression)
      return transferAssignVar(lhs, val);
    else if (lop instanceof FieldRef)
      return transferAssignField(lhs, val);
    else if (lop instanceof ArrayRefExpression)
      return transferAssignArray(lhs, val);
    else
      throw new FluidError("Left hand side of assignment is strange: " + lop);
  }

  /**
	 * Transfer evaluation over assignment of an array. (assuming array not null
	 * and index in bounds). <strong>leaf</leaf>
	 */
  protected Lattice<T> transferAssignArray(IRNode aref, Lattice<T> val) {
    return popSecond(popSecond(val)); // pop object and index
  }

  /**
	 * Transfer evaluation over assignment of a field (assuming object not null).
	 * <strong>leaf</leaf>
	 */
  protected Lattice<T> transferAssignField(IRNode fref, Lattice<T> val) {
    return popSecond(val); // pop object to assign into
  }

  /**
	 * Transfer evaluation over assignment of a variable. <strong>leaf</leaf>
	 */
  protected Lattice<T> transferAssignVar(IRNode var, Lattice<T> val) {
    return val; // do nothing
  }

  /**
	 * Transfer evaluation over use of binary operation <strong>major grouping
	 * </strong>
	 */
  protected Lattice<T> transferBinop(IRNode node, Operator op, Lattice<T> val) {
    if (ArithBinopExpression.prototype.includes(op)) {
      if (AddExpression.prototype.includes(op)) {
        return transferAdd(node, val);
      } else if (SubExpression.prototype.includes(op)) {
        return transferSubtract(node, val);
      } else if (MulExpression.prototype.includes(op)) {
        return transferMultiply(node, val);
      } else if (DivExpression.prototype.includes(op)) {
        return transferDivide(node, val);
      } else if (RemExpression.prototype.includes(op)) {
        return transferRemainder(node, val);
      }
    } else if (LogBinopExpression.prototype.includes(op)) {
      if (AndExpression.prototype.includes(op)) {
        return transferAnd(node, val);
      } else if (OrExpression.prototype.includes(op)) {
        return transferOr(node, val);
      } else if (XorExpression.prototype.includes(op)) {
        return transferXor(node, val);
      }
    } else if (ShiftExpression.prototype.includes(op)) {
      if (LeftShiftExpression.prototype.includes(op)) {
        return transferLeftShift(node, val);
      } else if (RightShiftExpression.prototype.includes(op)) {
        return transferRightShift(node, val);
      } else if (UnsignedRightShiftExpression.prototype.includes(op)) {
        return transferUnsignedRightShift(node, val);
      }
    }
    // Currently the following is an ArithBinopExpression operator
    // which is nonsensical, so we just test it out here:
    if (StringConcat.prototype.includes(op)) {
      return transferConcat(node, val);
    }
    throw new FluidError("Unknown Binop operator: " + op);
  }

  /**
	 * Transfer a lattice value over an array reference that is in bounds.
	 * 
	 * @param node
	 *          the array reference node
	 * @param flag
	 *          true if in bounds, false if out of bounds
	 */
  @Override
  protected Lattice<T> transferBoundsCheck(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    // by default return the same value if true, otherwise pop pending
    if (flag)
      return value;
    else
      return popAllPending(value);
  }

  /** Transfer over an implicit boxing operation.
   * <string>leaf</strong>
   */
  protected Lattice<T> transferBox(IRNode expr, Lattice<T> value) {
    return value;
  }
  
  /**
	 * Transfer a lattice value over a method or constructor call. <strong>major
	 * grouping</strong>
	 * 
	 * @param call
	 *          the tree node containing the call
	 * @param flag
	 *          true for normal termination, false for abrupt termination
	 */
  @Override
  protected Lattice<T> transferCall(IRNode call, boolean flag, Lattice<T> value) {
    if (flag == false) {
      return transferFailedCall(call, value);
    } else {
      return transferCall(call, value);
    }
  }

  /**
	 * Transfer a value over a successful call expression. <strong>leaf
	 * </strong>
	 */
  protected Lattice<T> transferCall(IRNode node, Lattice<T> value) {
    Operator op = tree.getOperator(node);
    boolean mcall = MethodCall.prototype.includes(op);
    IRNode actuals = ((CallInterface) op).get_Args(node);
    boolean q = hasOuterObject(node);
    // pop actuals
    value = pop(value, tree.numChildren(actuals));
    // if constructor, pop qualifications
    // while leaving receiver in place:
    if (q) {
      if (mcall) {
        LOG.severe("MethodCall's can't have qualifiers!");
      }
      value = popSecond(value);
    }
    // now if a method call, pop receiver and push return value
    if (mcall) {
      value = pop(value);
      value = push(value);
    }
    return value;
  }

  /**
	 * Transfer a value over a successful cast expression. <strong>major
	 * grouping, leaf</strong>
	 */
  protected Lattice<T> transferCast(IRNode node, Lattice<T> value) {
    // by default return the same value:
    return value;
  }

  @Override
  protected Lattice<T> transferCastExpression(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    if (flag)
      return transferCast(node, value);
    else
      return transferFailedCast(node, value);
  }

  /**
	 * Transfer evaluation over ".class" expression. <strong>major grouping,
	 * leaf</strong>
	 */
  protected Lattice<T> transferClassExpression(IRNode node, Lattice<T> val) {
    // push the class object on the stack.
    return push(val);
  }

  /**
   * Transfer evaluation over the end of a block statement.
   * By default, we simply return the lattice value unchanged.
   * <strong>major grouping, leaf</string>
   * @param node BlockStatement node
   * @param val lattice value to transfer
   * @return new lattice value after scope is closed.
   */
  protected Lattice<T> transferCloseScope(IRNode node, Lattice<T> val) {
    return val;
  }
  
  /**
	 * Transfer lattice value over complement unary operation. <strong>leaf
	 * </strong>
	 */
  protected Lattice<T> transferComplement(IRNode node, Lattice<T> val) {
    return push(pop(val));
  }

  /**
   * Transfer a lattice value over String addition. 
   * By default, @{link #transferToString} (which does a pop)
   * both things on the stack and push an unknown value. 
   * <strong>leaf</strong>
   */
  protected Lattice<T> transferConcat(IRNode node, Lattice<T> value) {
    return push(transferToString(node,transferToString(node,value)));
  }

  /**
	 * Transfer evaluation over (pre) {in}{de}crement operation. <strong>leaf
	 * </strong>
	 */
  protected Lattice<T> transferCrement(IRNode node, Operator op, Lattice<T> val) {
    return push(pop(val));
  }

  /**
	 * Transfer evaluation over an initialization <strong>major grouping, leaf
	 * </strong>
	 */
  protected Lattice<T> transferDefaultInit(IRNode node, Lattice<T> val) {
    return push(val);
  }

  /**
	 * Transfer a value over an array creation's dimensions. true branch should
	 * handle anything regarding allocating the array
	 * 
	 * @param dimExprs
	 *          the node containing the dimension expressions
	 * @param flag
	 *          whether all indices are nonnegative.
	 */
  @Override
  protected Lattice<T> transferDimsCheck(
    IRNode dimExprs,
    boolean flag,
    Lattice<T> value) {
    Lattice<T> val = super.transferDimsCheck(dimExprs, flag, value);
    if (val == null)
      return val;
    if (flag) {
      return transferArrayCreation(dimExprs, val);
    } else {
      return popAllPending(val);
    }
  }

  /**
	 * Transfer a value over a divide or remainder operation.
	 * 
	 * @param divisor
	 *          the node containing the divisor expression.
	 * @param flag
	 *          successful divide (divisor != 0)
	 */
  @Override
  protected Lattice<T> transferDivide(
    IRNode divisor,
    Operator op,
    boolean flag,
    Lattice<T> value) {
    if (flag)
      return transferBinop(tree.getParent(divisor), op, value);
    else
      return transferFailedDivide(divisor, value);
  }

  /**
	 * Transfer a value over a successful divide operation. <strong>leaf
	 * </strong>
	 */
  protected Lattice<T> transferDivide(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over == test. <strong>leaf</strong>
	 */
  protected Lattice<T> transferEq(IRNode node, boolean flag, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over an assignment into an array that fails
	 * because of ArrayStoreException.
	 * 
	 * @param node
	 *          the assignment node.
	 */
  @Override
  protected Lattice<T> transferFailedArrayStore(IRNode node, Lattice<T> value) {
    // by default pop pending values
    return popAllPending(value);
  }

  /**
	 * Transfer a lattice value over a call which raises an exception.
	 */
  protected Lattice<T> transferFailedCall(IRNode node, Lattice<T> value) {
    return popAllPending(value);
  }

  /**
	 * Transfer a lattice value over a failed cast expression, one that throws an
	 * exception.
	 */
  protected Lattice<T> transferFailedCast(IRNode node, Lattice<T> value) {
    return popAllPending(value);
  }

  /**
	 * Transfer a lattice value over a failed divide or remainder.
	 */
  protected Lattice<T> transferFailedDivide(IRNode divisor, Lattice<T> value) {
    return popAllPending(value);
  }

  /**
	 * Transfer a lattice value over &gt; test. <strong>leaf</strong>
	 */
  protected Lattice<T> transferGreater(IRNode node, boolean flag, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over &gt;= test. <strong>leaf</strong>
	 */
  protected Lattice<T> transferGreaterEqual(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    return push(pop(pop(value)));
  }

  @Override
  protected Lattice<T> transferImplicitArrayCreation(
    IRNode arrayInitializer,
    Lattice<T> value) {
    return transferArrayCreation(arrayInitializer, value);
  }

  /**
	 * Transfer a lattice value over a local initialization. <strong>major
	 * grouping</strong>
	 * 
	 * @param node
	 *          the initialization node.
	 */
  @Override
  protected Lattice<T> transferInitialization(IRNode node, Lattice<T> value) {
    IRNode p = tree.getParent(tree.getParent(node));
    if (FieldDeclaration.prototype.includes(tree.getOperator(p))) {
      return transferInitializationOfField(node, value);
    } else {
      return transferInitializationOfVar(node, value);
    }
  }

  /**
	 * Transfer a lattice value over a local initialization. <strong>leaf
	 * </strong>
	 * 
	 * @param node
	 *          the field declarator node.
	 */
  protected Lattice<T> transferInitializationOfField(IRNode node, Lattice<T> value) {
    return pop(value); // value is stored
  }

  /**
	 * Transfer a lattice value over a local initialization. <strong>leaf
	 * </strong>
	 * 
	 * @param node
	 *          the declarator node
	 */
  protected Lattice<T> transferInitializationOfVar(IRNode node, Lattice<T> value) {
    return pop(value); // value is stored
  }

  /**
	 * Transfer evaluation over <tt>instanceof</tt> test. <strong>major
	 * grouping</strong>
	 */
  protected Lattice<T> transferInstanceOf(
    IRNode node,
    boolean flag,
    Lattice<T> val) {
    return push(pop(val));
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
  @Override
  protected Lattice<T> transferIsObject(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    // by default, return value if true
    // but pop pending if false
    if (flag)
      return value;
    else
      return popAllPending(value);
  }

  /**
	 * Transfer a lattice value over &lt;&lt; shift. <strong>leaf</strong>
	 */
  protected Lattice<T> transferLeftShift(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over &lt; test. <strong>leaf</strong>
	 */
  protected Lattice<T> transferLess(IRNode node, boolean flag, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over &lt;= test. <strong>leaf</strong>
	 */
  protected Lattice<T> transferLessEqual(
    IRNode node,
    boolean flag,
    Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
   * Transfer evaluation over literal expression. <strong>leaf</strong>
   */
  protected Lattice<T> transferLiteral(IRNode node, Lattice<T> val) {
    return push(val);
  }

  /**
	 * Transfer a lattice value over a unary minus operation. <strong>leaf
	 * </leaf>
	 */
  protected Lattice<T> transferMinus(IRNode node, Lattice<T> value) {
    return push(pop(value));
  }

  /**
	 * Transfer lattice value over a synchronized statement enter or exit. If
	 * entering, the lock is on the stack. Otherwise it is not. If the analysis
	 * wants to keep track of locking, it will need an auxiliary lock stack (not
	 * the evaluation stack). <strong>leaf</leaf>
	 * 
	 * @param node
	 *          the synchronized statement
	 * @param enter
	 *          true for entering, false for exiting
	 */
  @Override
  protected Lattice<T> transferMonitorAction(
    IRNode node,
    boolean enter,
    Lattice<T> value) {
    if (enter)
      return pop(value);
    else
      return value;
  }

  /**
	 * Transfer a lattice value over multiplication. <strong>leaf</strong>
	 */
  protected Lattice<T> transferMultiply(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over | connective. <strong>leaf</strong>
	 */
  protected Lattice<T> transferOr(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over a unary plus operation (that is, widen
	 * chars, bytes and shorts to ints). <strong>leaf</leaf>
	 */
  protected Lattice<T> transferPlus(IRNode node, Lattice<T> value) {
    return push(pop(value));
  }

  /** Transfer over a test */
  @Override
  protected Lattice<T> transferRelop(
    IRNode node,
    Operator op,
    boolean flag,
    Lattice<T> value) {
    if (EqualityExpression.prototype.includes(op)) {
      if (EqExpression.prototype.includes(op)) {
        return transferEq(node, flag, value);
      } else if (NotEqExpression.prototype.includes(op)) {
        return transferEq(node, !flag, value); // .!=. equiv !(.==.)
      }
    } else if (CompareExpression.prototype.includes(op)) {
      if (GreaterThanExpression.prototype.includes(op)) {
        return transferGreater(node, flag, value);
      } else if (GreaterThanEqualExpression.prototype.includes(op)) {
        return transferGreaterEqual(node, flag, value);
      } else if (LessThanExpression.prototype.includes(op)) {
        return transferLess(node, flag, value);
      } else if (LessThanEqualExpression.prototype.includes(op)) {
        return transferLessEqual(node, flag, value);
      }
    } else if (InstanceOfExpression.prototype.includes(op)) {
      return transferInstanceOf(node, flag, value);
    }
    throw new FluidError("unknown relop " + op);
  }

  /**
	 * Transfer a lattice value over % operation. <strong>leaf</strong>
	 */
  protected Lattice<T> transferRemainder(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer evaluation over assigning an exoression to return value. <strong>
	 * major grouping, leaf</strong>
	 */
  protected Lattice<T> transferReturn(IRNode node, Lattice<T> val) {
    return pop(val);
  }

  /**
	 * Transfer a lattice value over &gt;&gt; shift. <strong>leaf</strong>
	 */
  protected Lattice<T> transferRightShift(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over subtraction. <strong>leaf</strong>
	 */
  protected Lattice<T> transferSubtract(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer evaluation over assigning expression to switch temporary <strong>
	 * major grouping, leaf</strong>
	 */
  protected Lattice<T> transferSwitch(IRNode node, Lattice<T> val) {
    return pop(val);
  }

  /**
	 * Transfer evaluation over assigning expression to exception temp. <strong>
	 * major grouping, leaf</strong>
	 */
  protected Lattice<T> transferThrow(IRNode node, Lattice<T> val) {
    return pop(val);
  }

  /**
   * Transfer evaluation over coercoing an operand to a string.
   * By default, we just pop.
   * <strong>leaf</strong>
   */
  protected Lattice<T> transferToString(IRNode node, Lattice<T> val) {
    return pop(val);
  }


  /**
	 * Transfer evaluation over using a type as an expression. <strong>major
	 * grouping, leaf</strong>
	 */
  protected Lattice<T> transferType(IRNode node, Lattice<T> val) {
    return push(val);
  }

  /** Transfer over an implicit unboxing operation
   * after checking for non-null.
   * <string>leaf</strong>
   */
  protected Lattice<T> transferUnbox(IRNode expr, Lattice<T> value) {
    return value;
  }
  
  /**
	 * Transfer evaluation over unary operation. <strong>major grouping
	 * </strong>
	 */
  protected Lattice<T> transferUnop(IRNode node, Operator op, Lattice<T> val) {
    if (ArithUnopExpression.prototype.includes(op)) {
      if (CrementExpression.prototype.includes(op)) {
        return transferCrement(node, op, val);
      } else if (MinusExpression.prototype.includes(op)) {
        return transferMinus(node, val);
      } else if (PlusExpression.prototype.includes(op)) {
        return transferPlus(node, val);
      }
    } else if (ComplementExpression.prototype.includes(op)) {
      return transferComplement(node, val);
    } else if (BoxExpression.prototype.includes(op)) {
      return transferBox(node,val);
    } else if (UnboxExpression.prototype.includes(op)) {
      return transferUnbox(node,val);
    }
    // NB: NotExpression is accomplished with CFG edges and boolean ports.
    throw new FluidError("unknown unary operation " + op);
  }

  /**
	 * Transfer a lattice value over unsigned right shift. <strong>leaf
	 * </strong>
	 */
  protected Lattice<T> transferUnsignedRightShift(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
	 * Transfer a lattice value over a use of a local, parameter, receiver,
	 * field, or array element. This is <em>not</em> called in lvalue
	 * situations. <strong>major grouping</strong>
	 * 
	 * @param node
	 *          the use being examined.
	 */
  @Override
  protected Lattice<T> transferUse(IRNode node, Operator op, Lattice<T> value) {
    if (op instanceof VariableUseExpression || op instanceof ThisExpression)
      return transferUseVar(node, value);
    else if (op instanceof FieldRef) 
      return transferUseField(node, value);
    else if (op instanceof ArrayLength) 
      return transferUseArrayLength(node, value);
    else if (op instanceof ArrayRefExpression)
      return transferUseArray(node, value);
    else if (
       op instanceof SuperExpression || op instanceof QualifiedThisExpression
       || op instanceof QualifiedSuperExpression)
         return transferUseVar(node,value);
    else
      throw new FluidError("use is strange: " + op);
  }

  /**
	 * Return whether this node is on the LHS of an OpAssignExpression. If so, it
	 * means the object will have to be duplicated on the stack to handle the
	 * latest assignment.
	 * 
	 * @see #transferUseField
	 * @see #transferUseArray
	 * @see edu.cmu.cs.fluid.java.operator.OpAssignExpression @precondition node !=
	 *      null
	 */
  protected boolean isBothLhsRhs(IRNode node) {
    IRNode p = tree.getParentOrNull(node);
    final boolean returnVal =
      (p != null
        && (tree.getOperator(p) instanceof CrementExpression
          || (tree.getOperator(p) instanceof OpAssignExpression
            && node.equals(tree.getChild(p, 0)))));
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("isBothLhsRhs: node = " + DebugUnparser.toString(node));
      LOG.fine("isBothLhsRhs: p = " + DebugUnparser.toString(p));
      LOG.fine("isBothLhsRhs: returnVal = " + returnVal);
    }
    return returnVal;
  }

  /**
	 * Transfer evaluation over use of an array. (assuming array not null and
	 * index in bounds). We must duplicate both array and index. <strong>leaf
	 * </leaf>
	 */
  protected Lattice<T> transferUseArray(IRNode aref, Lattice<T> val) {
    if (isBothLhsRhs(aref))
      val = push(push(val));
    return push(pop(pop(val)));
  }

  /**
	 * Transfer evaluation over use of a field (assuming object not null).
	 * <strong>leaf</leaf>
	 */
  protected Lattice<T> transferUseField(IRNode fref, Lattice<T> val) {
    if (isBothLhsRhs(fref))
      val = push(val);
    return push(pop(val));
  }

  /**
   * Transfer evaluation over use of a array .length (assuming object not null).
   * <strong>leaf</leaf>
   */
  protected Lattice<T> transferUseArrayLength(IRNode fref, Lattice<T> val) {
    if (isBothLhsRhs(fref))
      val = push(val);
    return push(pop(val));
  }
  
  /**
	 * Transfer evaluation over use of a variable. <strong>leaf</leaf>
	 */
  protected Lattice<T> transferUseVar(IRNode var, Lattice<T> val) {
    return push(val);
  }

  /**
   * Transfer a lattice value over ^ connective. <strong>leaf </strong>
   */
  protected Lattice<T> transferXor(IRNode node, Lattice<T> value) {
    return push(pop(pop(value)));
  }

  /**
   * Return number of OuterObjectSpecifiers nodes wrapped around this
   * allocation.  The result is either 0 or 1.
   */
  protected final boolean hasOuterObject(IRNode node) {
    IRNode p = tree.getParent(node);
    IRNode gp = tree.getParent(node);
    return (tree.getOperator(p) instanceof OuterObjectSpecifier ||
                  tree.getOperator(gp) instanceof OuterObjectSpecifier);
  }
}
