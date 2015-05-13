package com.surelogic.annotation.parse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;

/**
from JLS 7 sec. 15.28 p. 536
============================
A compile-time constant expression is an expression denoting a value of primitive
type or a String that does not complete abruptly and is composed using only the
following:
* Literals of primitive type and literals of type String (JLS 3.10.1, JLS 3.10.2, JLS 3.10.3,
JLS 3.10.4, JLS 3.10.5)
* (omitted)
* The unary operators +, -, ~, and ! (but not ++ or --) (JLS 15.15.3, JLS 15.15.4, JLS 15.15.5,
JLS 15.15.6)
* (omitted)
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
public final class ConstantBooleanExprVisitor extends ConstantExprVisitor<Boolean> {
	ConstantBooleanExprVisitor(IBinder b) {
		super(b);
	}

	@Override
	public Boolean visitFalseExpression(IRNode node) {
		return Boolean.FALSE;
	}
		
	@Override
	public Boolean visitTrueExpression(IRNode node) {
		return Boolean.TRUE;
	}
	
	public Boolean visitCastExpression(IRNode node) {
		// TODO does it have to be a boolean?
		return doAccept(CastExpression.getExpr(node));
	}
	
	@Override
	public Boolean visitNotExpression(IRNode node) {
		return doAccept(NotExpression.getOp(node)) ? Boolean.FALSE : Boolean.TRUE;
	}
	
	/*
	@Override
	public Boolean visitEqExpression(IRNode node) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Boolean visitNotEqExpression(IRNode node) {
		throw new UnsupportedOperationException();
	}
	*/
	
	@Override
	public Boolean visitAndExpression(IRNode node) {
		return doAccept(AndExpression.getOp1(node)) & doAccept(AndExpression.getOp2(node));
	}
	
	@Override
	public Boolean visitOrExpression(IRNode node) {
		return doAccept(OrExpression.getOp1(node)) | doAccept(OrExpression.getOp2(node));
	}
	
	@Override
	public Boolean visitConditionalAndExpression(IRNode node) {
		return doAccept(ConditionalAndExpression.getOp1(node)) && doAccept(ConditionalAndExpression.getOp2(node));
	}
	
	@Override
	public Boolean visitConditionalOrExpression(IRNode node) {
		return doAccept(ConditionalOrExpression.getOp1(node)) || doAccept(ConditionalOrExpression.getOp2(node));
	}
	
	@Override
	public Boolean visitConditionalExpression(IRNode node) {
		boolean cond = doAccept(ConditionalExpression.getCond(node));
		return cond ? doAccept(ConditionalExpression.getIftrue(node)) : 
			          doAccept(ConditionalExpression.getIffalse(node));
	}
}
