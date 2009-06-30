package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Lattice;

/**
 * @author Scott Wisniewski
 */
public class EvaluationTransferAdapter<T,V> {
	private JavaEvaluationTransfer<T,V> wrapped_;
	
	public EvaluationTransferAdapter(JavaEvaluationTransfer<T,V> wrapped) {
		wrapped_ = wrapped;		
	}

	public Lattice<T> dup(Lattice<T> val) {
		return wrapped_.dup(val);
	}
	
	public boolean isBothLhsRhs(IRNode node) {
		return wrapped_.isBothLhsRhs(node);
	}
	
	public Lattice<T> pop(Lattice<T> val, int n) {
		
		return wrapped_.pop(val, n);
	}

	public Lattice<T> pop(Lattice<T> val) {
		
		return wrapped_.pop(val);
	}

	public Lattice<T> popAllPending(Lattice<T> val) {
		
		return wrapped_.popAllPending(val);
	}

	public Lattice<T> popSecond(Lattice<T> val) {
		
		return wrapped_.popSecond(val);
	}

	public Lattice<T> push(Lattice<T> val) {
		
		return wrapped_.push(val);
	}

	public Lattice<T> transferAdd(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferAdd(node, value);
	}

	public Lattice<T> transferAllocation(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferAllocation(node, value);
	}

	public Lattice<T> transferAnd(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferAnd(node, value);
	}

	public Lattice<T> transferAnonClass(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferAnonClass(node, value);
	}

	public Lattice<T> transferArrayCreation(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferArrayCreation(node, val);
	}

	public Lattice<T> transferArrayInitializer(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferArrayInitializer(node, val);
	}

	public Lattice<T> transferAssignArray(IRNode aref, Lattice<T> val) {
		
		return wrapped_.transferAssignArray(aref, val);
	}

	public Lattice<T> transferAssignField(IRNode fref, Lattice<T> val) {
		
		return wrapped_.transferAssignField(fref, val);
	}

	public Lattice<T> transferAssignment(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferAssignment(node, val);
	}

	public Lattice<T> transferAssignVar(IRNode var, Lattice<T> val) {
		
		return wrapped_.transferAssignVar(var, val);
	}

	public Lattice<T> transferBinop(IRNode node, Operator op, Lattice<T> val) {
		
		return wrapped_.transferBinop(node, op, val);
	}

	public Lattice<T> transferBoundsCheck(IRNode node, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferBoundsCheck(node, flag, value);
	}

	public Lattice<T> transferCall(IRNode call, boolean flag, Lattice<T> value) {
		
		return wrapped_.transferCall(call, flag, value);
	}

	public Lattice<T> transferCall(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferCall(node, value);
	}

	public Lattice<T> transferCast(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferCast(node, value);
	}

	public Lattice<T> transferCastExpression(IRNode node, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferCastExpression(node, flag, value);
	}

	public Lattice<T> transferClassExpression(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferClassExpression(node, val);
	}
	
	public Lattice<T> transferComplement(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferComplement(node, val);
	}

	public Lattice<T> transferCrement(IRNode node, Operator op, Lattice<T> val) {
		
		return wrapped_.transferCrement(node, op, val);
	}

	public Lattice<T> transferDefaultInit(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferDefaultInit(node, val);
	}
	

	public Lattice<T> transferDimsCheck(IRNode dimExprs, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferDimsCheck(dimExprs, flag, value);
	}

	public Lattice<T> transferDivide(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferDivide(node, value);
	}

	public Lattice<T> transferDivide(IRNode divisor, Operator op, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferDivide(divisor, op, flag, value);
	}

	public Lattice<T> transferEq(IRNode node, boolean flag, Lattice<T> value) {
		
		return wrapped_.transferEq(node, flag, value);
	}

	public Lattice<T> transferFailedArrayStore(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferFailedArrayStore(node, value);
	}

	public Lattice<T> transferFailedCall(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferFailedCall(node, value);
	}

	public Lattice<T> transferFailedCast(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferFailedCast(node, value);
	}

	public Lattice<T> transferFailedDivide(IRNode divisor, Lattice<T> value) {
		
		return wrapped_.transferFailedDivide(divisor, value);
	}

	public Lattice<T> transferGreater(IRNode node, boolean flag, Lattice<T> value) {
		
		return wrapped_.transferGreater(node, flag, value);
	}

	public Lattice<T> transferGreaterEqual(IRNode node, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferGreaterEqual(node, flag, value);
	}

	public Lattice<T> transferImplicitArrayCreation(IRNode arrayInitializer,
			Lattice<T> value) {
		
		return wrapped_.transferImplicitArrayCreation(arrayInitializer, value);
	}
	
	public Lattice<T> transferInitialization(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferInitialization(node, value);
	}

	public Lattice<T> transferInitializationOfField(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferInitializationOfField(node, value);
	}

	public Lattice<T> transferInitializationOfVar(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferInitializationOfVar(node, value);
	}

	public Lattice<T> transferInstanceOf(IRNode node, boolean flag, Lattice<T> val) {
		
		return wrapped_.transferInstanceOf(node, flag, val);
	}

	public Lattice<T> transferIsObject(IRNode node, boolean flag, Lattice<T> value) {
		
		return wrapped_.transferIsObject(node, flag, value);
	}

	public Lattice<T> transferLeftShift(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferLeftShift(node, value);
	}

	public Lattice<T> transferLess(IRNode node, boolean flag, Lattice<T> value) {
		
		return wrapped_.transferLess(node, flag, value);
	}

	public Lattice<T> transferLessEqual(IRNode node, boolean flag, Lattice<T> value) {
		
		return wrapped_.transferLessEqual(node, flag, value);
	}

	public Lattice<T> transferLiteral(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferLiteral(node, val);
	}

	public Lattice<T> transferMinus(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferMinus(node, value);
	}

	public Lattice<T> transferMonitorAction(IRNode node, boolean enter,
			Lattice<T> value) {
		
		return wrapped_.transferMonitorAction(node, enter, value);
	}

	public Lattice<T> transferMultiply(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferMultiply(node, value);
	}

	public Lattice<T> transferOr(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferOr(node, value);
	}

	public Lattice<T> transferPlus(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferPlus(node, value);
	}

	public Lattice<T> transferRelop(IRNode node, Operator op, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferRelop(node, op, flag, value);
	}

	public Lattice<T> transferRemainder(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferRemainder(node, value);
	}

	public Lattice<T> transferReturn(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferReturn(node, val);
	}

	public Lattice<T> transferRightShift(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferRightShift(node, value);
	}

	public Lattice<T> transferSubtract(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferSubtract(node, value);
	}

	public Lattice<T> transferSwitch(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferSwitch(node, val);
	}

	public Lattice<T> transferThrow(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferThrow(node, val);
	}

	public Lattice<T> transferType(IRNode node, Lattice<T> val) {
		
		return wrapped_.transferType(node, val);
	}

	public Lattice<T> transferUnop(IRNode node, Operator op, Lattice<T> val) {
		
		return wrapped_.transferUnop(node, op, val);
	}

	public Lattice<T> transferUnsignedRightShift(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferUnsignedRightShift(node, value);
	}

	public Lattice<T> transferUse(IRNode node, Operator op, Lattice<T> value) {
		
		return wrapped_.transferUse(node, op, value);
	}

	public Lattice<T> transferUseArray(IRNode aref, Lattice<T> val) {
		
		return wrapped_.transferUseArray(aref, val);
	}

	public Lattice<T> transferUseField(IRNode fref, Lattice<T> val) {
		
		return wrapped_.transferUseField(fref, val);
	}

	public Lattice<T> transferUseVar(IRNode var, Lattice<T> val) {
		
		return wrapped_.transferUseVar(var, val);
	}

	public Lattice<T> transferXor(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferXor(node, value);
	}

	public Lattice<T> transferConditional(IRNode node, boolean flag, Lattice<T> before) {
		
		return wrapped_.transferConditional(node, flag, before);
	}

	public Lattice<T> transferLabelTest(IRNode node, Object info,
			ControlLabel label, boolean flag, Lattice<T> before) {
		
		return wrapped_.transferLabelTest(node, info, label, flag, before);
	}

	public Lattice<T> transferComponentFlow(IRNode node, Object info, Lattice<T> value) {
		
		return wrapped_.transferComponentFlow(node, info, value);
	}

	public Lattice<T> transferComponentChoice(IRNode node, Object info,
			boolean flag, Lattice<T> value) {
		
		return wrapped_.transferComponentChoice(node, info, flag, value);
	}

	public boolean isStaticUse(IRNode ref) {
		
		return wrapped_.isStaticUse(ref);
	}

	public Lattice<T> transferCallClassInitializer(IRNode call, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferCallClassInitializer(call, flag, value);
	}

	public Lattice<T> transferParens(IRNode node, Lattice<T> value) {
		
		return wrapped_.transferParens(node, value);
	}

	public Lattice<T> transferCaseMatch(IRNode label, boolean flag,
			Lattice<T> value) {
		
		return wrapped_.transferCaseMatch(label, flag, value);
	}

	public Lattice<T> transferMethodBody(IRNode node, Port kind, Lattice<T> value) {
		
		return wrapped_.transferMethodBody(node, kind, value);
	}

	public Lattice<T> runClassInitializer(IRNode caller, IRNode classBody,
			Lattice<T> initial, boolean terminationNormal) {
		
		return wrapped_.runClassInitializer(caller, classBody, initial,
				terminationNormal);
	}
}
