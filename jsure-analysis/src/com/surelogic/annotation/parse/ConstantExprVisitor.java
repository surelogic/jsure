package com.surelogic.annotation.parse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.operator.*;

/** from JLS 7 sec. 9.7.1 p. 287
 * =============================
 * The type of V is assignment compatible (§5.2) with T, and furthermore:
   If T is a primitive type or String, and V is a constant expression (§15.28).
   V is not null.
   If T is Class, or an invocation of Class, and V is a class literal (§15.8.2).
   If T is an enum type, and V is an enum constant.
   
   from JLS 7 sec. 15.28 p. 536
   ============================
   A compile-time constant expression is an expression denoting a value of primitive
type or a String that does not complete abruptly and is composed using only the
following:
• Literals of primitive type and literals of type String (§3.10.1, §3.10.2, §3.10.3,
§3.10.4, §3.10.5)
• Casts to primitive types and casts to type String (§15.16)
• The unary operators +, -, ~, and ! (but not ++ or --) (§15.15.3, §15.15.4, §15.15.5,
§15.15.6)
• The multiplicative operators *, /, and % (§15.17)
• The additive operators + and - (§15.18)
• The shift operators <<, >>, and >>> (§15.19)
• The relational operators <, <=, >, and >= (but not instanceof) (§15.20)
• The equality operators == and != (§15.21)
• The bitwise and logical operators &, ^, and | (§15.22)
• The conditional-and operator && and the conditional-or operator || (§15.23,
§15.24)
• The ternary conditional operator ? : (§15.25)
• Parenthesized expressions (§15.8.5) whose contained expression is a constant
expression.
• Simple names (§6.5.6.1) that refer to constant variables (§4.12.4).
• Qualified names (§6.5.6.2) of the form TypeName . Identifier that refer to
constant variables (§4.12.4). 
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
