package com.surelogic.annotation.parse;

import com.surelogic.javac.adapter.SourceAdapter;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalExpression;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;

/**
   from JLS 7 sec. 15.28 p. 536
   ============================
   A compile-time constant expression is an expression denoting a value of primitive
type or a String that does not complete abruptly and is composed using only the
following:
• Literals of primitive type and literals of type String (§3.10.1, §3.10.2, §3.10.3,
§3.10.4, §3.10.5)
• Casts to primitive types and casts to type String (§15.16)
• The additive operators + and - (§15.18)
• The ternary conditional operator ? : (§15.25)
• Parenthesized expressions (§15.8.5) whose contained expression is a constant
expression.
• Simple names (§6.5.6.1) that refer to constant variables (§4.12.4).
• Qualified names (§6.5.6.2) of the form TypeName . Identifier that refer to
constant variables (§4.12.4). 
 */
public class ConstantStringExprVisitor extends ConstantExprVisitor<String> {	
	ConstantStringExprVisitor(IBinder b) {
		super(b);
	}
	
	public String visitStringLiteral(IRNode node) {
		String c = StringLiteral.getToken(node);
		if (SourceAdapter.includeQuotesInStringLiteral) {
			final boolean quoteStart = c.startsWith("\"");    	  
			if (quoteStart && c.endsWith("\"")) {
				c = c.substring(1, c.length() - 1);

			}
			/*
	    	  else if (!quoteStart) {
	    		  System.out.println("String literal without quotes");          
	    	  }
			 */
		}      
		return c;
	}

	public String visitLiteralExpression(IRNode node) {
		return DebugUnparser.toString(node);
	}
	
	public String visitBoxExpression(IRNode node) {
		return doAccept(BoxExpression.getOp(node));
	}
	
	public String visitMethodCall(IRNode node) {
		if ("toString".equals(MethodCall.getMethod(node))) {
			return doAccept(MethodCall.getObject(node));
		}
		return super.visitMethodCall(node);
	}
 	
	public String visitCastExpression(IRNode node) {
		// TODO does it have to be a String?
		return doAccept(CastExpression.getExpr(node));
	}
	
	public String visitStringConcat(IRNode node) {
		return doAccept(StringConcat.getOp1(node)) +
	           doAccept(StringConcat.getOp2(node));
	}
	
	@Override
	public String visitConditionalExpression(IRNode node) {
		boolean cond = getBooleanVisitor().doAccept(ConditionalExpression.getCond(node));
		return cond ? doAccept(ConditionalExpression.getIftrue(node)) : 
			          doAccept(ConditionalExpression.getIffalse(node));
	}
}
