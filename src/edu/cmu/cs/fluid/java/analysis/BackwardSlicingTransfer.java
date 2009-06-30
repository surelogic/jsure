/*
 * Created on May 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

import edu.cmu.cs.fluid.java.operator.*;
//import edu.cmu.cs.fluid.java.operator.AddExpression;
//import edu.cmu.cs.fluid.java.operator.Assignment;
//import edu.cmu.cs.fluid.java.operator.ExprStatement;
//import edu.cmu.cs.fluid.java.operator.FieldRef;
//import edu.cmu.cs.fluid.java.operator.ForStatement;
//import edu.cmu.cs.fluid.java.operator.LiteralExpression;
//import edu.cmu.cs.fluid.java.operator.ReturnStatement;
//import edu.cmu.cs.fluid.java.operator.Statement;
//mport edu.cmu.cs.fluid.java.operator.StatementExpression;
//import edu.cmu.cs.fluid.java.operator.StatementExpressionInterface;
//import edu.cmu.cs.fluid.java.operator.UseExpression;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.BooleanLattice;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.PairLattice;
import edu.cmu.cs.fluid.util.SetLattice;
import edu.cmu.cs.fluid.util.StackLattice;
import edu.cmu.cs.fluid.util.SetException;

//import java.util.Hashtable;
import java.util.TreeSet;


public class BackwardSlicingTransfer<V> extends JavaBackwardTransfer<PairLattice.Type<IRNode,Boolean>,V> {
  protected TreeSet<IRNode> slice;
  protected TreeSet<IRNode> criterionVariables;
  
	/**
	 * @param base
	 * @param binder
	 */
	public BackwardSlicingTransfer(IntraproceduralAnalysis<PairLattice.Type<IRNode,Boolean>,V> base, IBinder binder) 
	{
		super(base, binder);
		slice = new TreeSet<IRNode>();
		criterionVariables = new TreeSet<IRNode>();
	}
	

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferUse(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice<PairLattice.Type<IRNode,Boolean>> transferUse(IRNode node, Operator op, Lattice<PairLattice.Type<IRNode,Boolean>> value) 
	{
		PairLattice<IRNode,Boolean> pValue = (PairLattice<IRNode,Boolean>)value;
		@SuppressWarnings("unused") SetLattice<IRNode> set = (SetLattice<IRNode>) pValue.getLeft();
		@SuppressWarnings("unused") StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		Lattice<PairLattice.Type<IRNode,Boolean>> newValue;
		
		if (op instanceof VariableUseExpression || op instanceof ThisExpression)
      newValue = transferUseVar(node, value);
    else if (op instanceof FieldRef)
      newValue = transferUseField(node, value);
    //else if (op instanceof ArrayRefExpression)
      //newValue = transferUseArray(node, value);
    else if (
       op instanceof SuperExpression || op instanceof QualifiedThisExpression
       || op instanceof QualifiedSuperExpression)
         newValue = transferUseVar(node,value);
    else
      throw new FluidError("use is strange: " + op);
    IRNode decl = binder.getBinding(node);
		if (!criterionVariables.contains(decl)) criterionVariables.add(decl);
		slice.add(node);
		return newValue;
	}
	protected boolean isBothLhsRhs(IRNode node) {
    IRNode p = tree.getParentOrNull(node);
    final boolean returnVal =
      (p != null
        && (tree.getOperator(p) instanceof CrementExpression
          || (tree.getOperator(p) instanceof OpAssignExpression
            && node.equals(tree.getChild(p, 0)))));
    return returnVal;
  }
	protected Lattice<PairLattice.Type<IRNode,Boolean>> transferUseArray(IRNode aref, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
		PairLattice pValue = (PairLattice)value;
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
    if (isBothLhsRhs(aref))
     { 
      stack.pop(); stack.pop();
     }  
    BooleanLattice e = (BooleanLattice)stack.peek();
	  IRNode decl = binder.getBinding(aref);
	  if (e == BooleanLattice.orLattice.bottom())
			return new PairLattice<IRNode,Boolean>((Lattice<IRNode>)set.addElement(decl),stack.pop());
		else return new PairLattice<IRNode,Boolean>(set,stack.pop());	  
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferUseField(IRNode fref, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
    if (isBothLhsRhs(fref)) stack.pop();
    stack.pop();
    BooleanLattice e = (BooleanLattice)stack.peek();
	  IRNode decl = binder.getBinding(fref);
	  if (e == BooleanLattice.orLattice.bottom())
			return new PairLattice<IRNode,Boolean>((Lattice<IRNode>)set.addElement(decl),stack.pop());
		else return new PairLattice<IRNode,Boolean>(set,stack.pop());
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferUseVar(IRNode var, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
  	PairLattice pValue = (PairLattice)value;
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		BooleanLattice e = (BooleanLattice)stack.peek();
	  IRNode decl = binder.getBinding(var);
	  if (e == BooleanLattice.orLattice.bottom())
			return new PairLattice<IRNode,Boolean>((Lattice<IRNode>)set.addElement(decl),stack.pop());
		else return new PairLattice<IRNode,Boolean>(set,stack.pop());
  }
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferAssignment(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice<PairLattice.Type<IRNode,Boolean>> transferAssignment(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value)
	{
		PairLattice pValue = (PairLattice)value;
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		
	  IRNode target = ((AssignmentInterface)tree.getOperator(node)).getTarget(node);
	  IRNode decl = binder.getBinding(target);
	  PairLattice<IRNode,Boolean> newVal;
	  
	  if (VariableUseExpression.prototype.includes(tree.getOperator(target)))
	    {
	     if (set.contains(decl))
	       newVal = new PairLattice<IRNode,Boolean>((Lattice<IRNode>)set.removeElement(decl),stack.push(BooleanLattice.orLattice.bottom()));
       else
      	 newVal = new PairLattice<IRNode,Boolean>(set,stack.push(BooleanLattice.orLattice.top()));
	    } 
	  else 
	    newVal = new PairLattice<IRNode,Boolean>(set,stack.push(BooleanLattice.orLattice.top()));

		if (criterionVariables.contains(decl)) slice.add(node);
		return newVal;
	}
	protected Lattice<PairLattice.Type<IRNode,Boolean>> transferAdd(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
		PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferSubtract(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferMultiply(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferDivide(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  @Override protected Lattice<PairLattice.Type<IRNode,Boolean>> transferDivide(
    IRNode divisor,
    Operator op,
    boolean flag,
    Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    if (flag)
      return transferBinop(tree.getParent(divisor), op, value);
    else
      return transferFailedDivide(divisor, value);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferAnd(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferOr(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferXor(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferRemainder(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferRightShift(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferUnsignedRightShift(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferLeftShift(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferInstanceOf(
    IRNode node,
    boolean flag,
    Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferType(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferEq(IRNode node, boolean flag, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferLess(IRNode node, boolean flag, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferLessEqual(
    IRNode node,
    boolean flag,
    Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferGreater(IRNode node, boolean flag, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferGreaterEqual(
    IRNode node,
    boolean flag,
    Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  /* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferRelop(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator, boolean, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice<PairLattice.Type<IRNode,Boolean>> transferRelop(IRNode node, Operator op, boolean flag,
			Lattice<PairLattice.Type<IRNode,Boolean>> value) 
	{
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
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferCast(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    return value;
  }
  @Override protected Lattice<PairLattice.Type<IRNode,Boolean>> transferCastExpression(
    IRNode node,
    boolean flag,
    Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    if (flag)
      return transferCast(node, value);
    else
      return transferFailedCast(node, value);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferBinop(IRNode node, Operator op, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    if (ArithBinopExpression.prototype.includes(op)) {
      if (AddExpression.prototype.includes(op)) {
        return transferAdd(node, value);
      } else if (SubExpression.prototype.includes(op)) {
        return transferSubtract(node, value);
      } else if (MulExpression.prototype.includes(op)) {
        return transferMultiply(node, value);
      } else if (DivExpression.prototype.includes(op)) {
        return transferDivide(node, value);
      } else if (RemExpression.prototype.includes(op)) {
        return transferRemainder(node, value);
      }
    } else if (LogBinopExpression.prototype.includes(op)) {
      if (AndExpression.prototype.includes(op)) {
        return transferAnd(node, value);
      } else if (OrExpression.prototype.includes(op)) {
        return transferOr(node, value);
      } else if (XorExpression.prototype.includes(op)) {
        return transferXor(node, value);
      }
    } else if (ShiftExpression.prototype.includes(op)) {
      if (LeftShiftExpression.prototype.includes(op)) {
        return transferLeftShift(node, value);
      } else if (RightShiftExpression.prototype.includes(op)) {
        return transferRightShift(node, value);
      } else if (UnsignedRightShiftExpression.prototype.includes(op)) {
        return transferUnsignedRightShift(node, value);
      }
    }
    throw new FluidError("Unknown Binop operator: " + op);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferCrement(IRNode node, Operator op, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferMinus(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferPlus(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferComplement(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.pop();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
	protected Lattice<PairLattice.Type<IRNode,Boolean>> transferUnop(IRNode node, Operator op, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    if (ArithUnopExpression.prototype.includes(op)) {
      if (CrementExpression.prototype.includes(op)) {
        return transferCrement(node, op, value);
      } else if (MinusExpression.prototype.includes(op)) {
        return transferMinus(node, value);
      } else if (PlusExpression.prototype.includes(op)) {
        return transferPlus(node, value);
      }
    } else if (ComplementExpression.prototype.includes(op)) {
      return transferComplement(node, value);
    }
    // NB: NotExpression is accomplished with CFG edges and boolean ports.
    throw new FluidError("unknown unary operation " + op);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferFailedCast(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		return new PairLattice<IRNode,Boolean>(set,stack.empty());
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferFailedDivide(IRNode divisor, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		return new PairLattice<IRNode,Boolean>(set,stack.empty());
  }
  @Override
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferInitialization(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    IRNode p = tree.getParent(tree.getParent(node));
    if (FieldDeclaration.prototype.includes(tree.getOperator(p))) {
      return transferInitializationOfField(node, value);
    } else {
      return transferInitializationOfVar(node, value);
    }
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferInitializationOfField(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferInitializationOfVar(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferReturn(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferThrow(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferSwitch(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		stack.push(BooleanLattice.orLattice.bottom());
    return new PairLattice<IRNode,Boolean>(set,stack);
  }
  @Override
  protected final Lattice<PairLattice.Type<IRNode,Boolean>> transferOperation(
      IRNode node,
      Operator op,
      Object info,
      Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    
    	PairLattice pValue = (PairLattice)value;
    	StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
			SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
      Lattice<PairLattice.Type<IRNode,Boolean>> newValue = null;
		
      if (Initializer.prototype.includes(op)) {
        if (LiteralExpression.prototype.includes(op)) {
          newValue = transferLiteral(node, value);
        } else if (PrimaryExpression.prototype.includes(op)) {
          if (info instanceof NewExpression) {
            /* allocation of new object */
            newValue = value;
          } else if (AnonClassExpression.prototype.includes(op)) {
            /* store finals used in bosy inside new object */
            newValue = value;
          } else if (TypeExpression.prototype.includes(op)) {
            newValue = value;
          }
        } else if (BinopExpression.prototype.includes(op)) {
          if (ConditionalAndExpression.prototype.includes(op)
            || ConditionalOrExpression.prototype.includes(op)) {
            /* discard value not causing short-circuit: */
            newValue = new PairLattice<IRNode,Boolean>(set, stack.pop());
          } else
            newValue = transferBinop(node, op, value);
        } else if (UnopExpression.prototype.includes(op)) {
          newValue = transferUnop(node, op, value);
        } else if (ArrayInitializer.prototype.includes(op)) {
          if (info == null) {
            /* possibly allocate memory */
            IRNode p = tree.getParentOrNull(node);
            if (p != null
              && !tree.getOperator(p).includes(ArrayCreationExpression.prototype)) {
              newValue = transferImplicitArrayCreation(node, value);
            } else {
              newValue = value;
            }
          } else {
            /* storing of individual elements */
            //newValue = transferArrayInitializer(node, value);
          }
        } else if (ConditionalExpression.prototype.includes(op)) {
          newValue = new PairLattice<IRNode,Boolean>(set, stack.pop()); // discard value without consideration
        } else if (ClassExpression.prototype.includes(op)) {
          newValue = value;
        } else if (ParenExpression.prototype.includes(op)) {
          newValue = value;
        }
      } else if (Statement.prototype.includes(op)) {
        if (/* discard values without consideration */
          AssertMessageStatement.prototype.includes(op)
            || AssertStatement.prototype.includes(op)
            || ConstructorCall.prototype.includes(op)
            || DoStatement.prototype.includes(op)
            || ExprStatement.prototype.includes(op)
            || ForStatement.prototype.includes(op)
            || IfStatement.prototype.includes(op)
            // || IfElseStatement.prototype.includes(op)
            || WhileStatement.prototype.includes(op)) {
          newValue = new PairLattice<IRNode,Boolean>(set, stack.pop());
        } else if (ReturnStatement.prototype.includes(op)) {
          newValue = transferReturn(node, value);
        } else if (SwitchStatement.prototype.includes(op)) {
          newValue = transferSwitch(node, value);
        } else if (ThrowStatement.prototype.includes(op)) {
          newValue = transferThrow(node, value);
        }
      } //else if (NoInitialization.prototype.includes(op)) {
        //newValue = transferDefaultInit(node, value);
      //} 
      else if (StatementExpressionList.prototype.includes(op)) {
        /* discard each expression as it is evaluated */
        newValue = new PairLattice<IRNode,Boolean>(set, stack.pop());
      }
      if (newValue == null) {
        throw new FluidError("No transition defined for " + op + " with info=" + info);
      }
      return newValue;
    }
  protected Lattice<PairLattice.Type<IRNode,Boolean>> transferLiteral(IRNode node, Lattice<PairLattice.Type<IRNode,Boolean>> value) {
    PairLattice pValue = (PairLattice)value;
    SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
		
    return new PairLattice<IRNode,Boolean>(set, stack.push(BooleanLattice.orLattice.bottom()));
  }
  @Override
  public Lattice<PairLattice.Type<IRNode,Boolean>> transferComponentFlow(IRNode node, Object info, Lattice<PairLattice.Type<IRNode,Boolean>> value) {

		PairLattice pValue = (PairLattice)value;
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
    Lattice<PairLattice.Type<IRNode,Boolean>> newValue;
		
		int SLVsize = set.size();
		
    Operator op = tree.getOperator(node);
    if (VariableUseExpression.prototype.includes(op)
      || FieldRef.prototype.includes(op)
      //|| ArrayRefExpression.prototype.includes(op)
      || ConstructionObject.prototype.includes(op)
      || QualifiedThisExpression.prototype.includes(op)) {
      IRNode parent = tree.getParent(node);
      if (parent != null
        && AssignExpression.prototype.includes(tree.getOperator(parent))
        && node == AssignExpression.getOp1(parent)) {
        // skip:
        newValue = value;
      } else {
        newValue = transferUse(node, op, value);
      }
    } else if (VariableDeclarator.prototype.includes(op)) {
      newValue = transferInitialization(node, value);
    } else if (ArrayInitializer.prototype.includes(op) && info == null) {
      /* allocate an array unless the child of an ArrayCreationExpression */
      IRNode parent = tree.getParent(node);
      if (parent != null
        && ArrayCreationExpression.prototype.includes(tree.getOperator(parent)))
        newValue = value;
      newValue = transferImplicitArrayCreation(node, value);
    } else if (SynchronizedStatement.prototype.includes(op)) {
      return transferMonitorAction(
        node,
        ((Boolean) info).booleanValue(),
        value);
    } else if (MethodBody.prototype.includes(op)) {
      newValue = value;
    } else if (info == null && op instanceof CrementExpression) {
      newValue = transferAssignment(node, value);
    } else {
      newValue = transferOperation(node, op, info, value);
    }
	
		SetLattice<IRNode> newSet = (SetLattice<IRNode>)((PairLattice)newValue).getLeft();
		if (SLVsize < newSet.size())
		{
		  for (int i=0; i<newSet.size(); i++)
		    try {
		    criterionVariables.add((IRNode) newSet.elementAt(i));
		    
		    } catch(SetException e) {}
		  slice.add(node);
		}
		  
		return newValue;
	}
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferIsObject(edu.cmu.cs.fluid.ir.IRNode, boolean, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice<PairLattice.Type<IRNode,Boolean>> transferIsObject(IRNode node, boolean flag, Lattice<PairLattice.Type<IRNode,Boolean>> value) 
	{
		PairLattice pValue = (PairLattice)value;
		SetLattice<IRNode> set = (SetLattice<IRNode>)pValue.getLeft();
		StackLattice<Boolean> stack = (StackLattice<Boolean>)pValue.getRight();
 
		StackLattice<Boolean> newStack = stack.pop();
		return new PairLattice<IRNode,Boolean>(set,newStack.push(BooleanLattice.orLattice.bottom()));
	}
	

}
