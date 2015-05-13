package com.surelogic.annotation.parse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.operator.*;

/** from JLS 7 sec. 9.7.1 p. 287
 * =============================
 * The type of V is assignment compatible (JLS 5.2) with T, and furthermore:
   If T is a primitive type or String, and V is a constant expression (JLS 15.28).
   V is not null.
   If T is Class, or an invocation of Class, and V is a class literal (JLS 15.8.2).
   If T is an enum type, and V is an enum constant.
   
   from JLS 7 sec. 15.28 p. 536
   ============================
   A compile-time constant expression is an expression denoting a value of primitive
type or a String that does not complete abruptly and is composed using only the
following:
* Literals of primitive type and literals of type String (JLS 3.10.1, JLS 3.10.2, JLS 3.10.3,
JLS 3.10.4, JLS 3.10.5)
* Casts to primitive types and casts to type String (JLS 15.16)
* The unary operators +, -, ~, and ! (but not ++ or --) (JLS 15.15.3, JLS 15.15.4, JLS 15.15.5,
JLS 15.15.6)
* The multiplicative operators *, /, and % (JLS 15.17)
* The additive operators + and - (JLS 15.18)
* The shift operators <<, >>, and >>> (JLS 15.19)
* The relational operators <, <=, >, and >= (but not instanceof) (JLS 15.20)
* The equality operators == and != (JLS 15.21)
* The bitwise and logical operators &, ^, and | (JLS 15.22)
* The conditional-and operator && and the conditional-or operator || (JLS 15.23,
JLS 15.24)
* The ternary conditional operator ? : (JLS 15.25)
* Parenthesized expressions (JLS 15.8.5) whose contained expression is a constant
expression.
* Simple names (JLS 6.5.6.1) that refer to constant variables (JLS 4.12.4).
* Qualified names (JLS 6.5.6.2) of the form TypeName . Identifier that refer to
constant variables (JLS 4.12.4). 
 */
public abstract class ConstantExprVisitor<T> extends Visitor<T> {
	private final IBinder binder;
	private ConstantBooleanExprVisitor boolVisitor = null;

	ConstantExprVisitor(IBinder b) {
		binder = b;
	}
	
	@Override
	public T visit(IRNode node) { 
		throw new UnsupportedOperationException("Unable to handle "+DebugUnparser.toString(node));
	}
	
	@Override
	public T visitFieldRef(IRNode node) {		
		// This has to have an initializer
		IBinding b = binder.getIBinding(node);
		IRNode init = VariableDeclarator.getInit(b.getNode());
		return doAccept(Initialization.getValue(init));
	}
	
	@Override
	public T visitParenExpression(IRNode node) {
		return doAccept(ParenExpression.getOp(node));
	}
	
	ConstantBooleanExprVisitor getBooleanVisitor() {
		if (boolVisitor == null) {
			boolVisitor = new ConstantBooleanExprVisitor(binder);
		}
		return boolVisitor;
	}
}
