package edu.cmu.cs.fluid.java.bind;

import com.surelogic.javac.adapter.SourceAdapter;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;

/**
 * Visitor that is used to determine if an expression is a "constant
 * expression" as defined in JLS �15.28.
 */
public class ConstantExpressionVisitor extends Visitor<Object> {
  private static final String NULL_AS_STRING = "\"null\"";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String TO_STRING = "toString";
  // Stand-in since null mean non-constant expr
  //private static final Object NULL_CONSTANT = new Object();
  private static final Object NO_CONST_VALUE = null;
  private final IBinder binder;
  
  
  
  public ConstantExpressionVisitor(final IBinder b) {
    binder = b;
  }

  
  
  @Override
  public Object visit(final IRNode n) {
    // Unless otherwise noted, expressions are not "constant"
    return NO_CONST_VALUE;
  }
  
  
  
  /*
   * Literals of primitive type and literals of type String
   * (�3.10.1,�3.10.2,�3.10.3, �3.10.4, �3.10.5)
   */
  
  @Override
  public Number visitIntLiteral(final IRNode e) {
	final String intStr = IntLiteral.getToken( e );
	return parseIntLiteral(intStr);
	/*
	//For a Integer Literal in java Language, if the end of it is 'L' or 'l', then it is long type, 
	//otherwise it is int type(not short or byte type). 
	final int length = intStr.length();
	final char last = intStr.charAt( length-1 );
	// TODO octal/hex?
	if( last == 'L' || last == 'l' ) {		
		return Long.parseLong(intStr.substring(0, length-1));
	}
    return Integer.parseInt(intStr);
    */
  }
  
  private Number parseIntLiteral(String token) {
	final Number i;
	if (token.endsWith("L") || token.endsWith("l")) {
        token = token.substring(0, token.length()-1);
        if (token.startsWith("0")) { // hex or octal?
          if (token.length() == 1) {
            i = 0;
          }
          else if (token.startsWith("0x")) { // hex
          i = Long.parseLong(token.substring(2), 16);
          }
          else {
            i = Long.parseLong(token.substring(1), 8);
          }
        } else {
          i = Long.parseLong(token);
        } 
      } else {
        if (token.startsWith("0")) { // hex or octal?
          if (token.length() == 1) {
            i = 0;
          }
          else if (token.startsWith("0x")) { // hex
          i = Integer.parseInt(token.substring(2), 16);
          }
          else {
            i = Integer.parseInt(token.substring(1), 8);
          }
        } else {
          i = Integer.parseInt(token);
        } 
      }
	return i;
  }
  
  @Override
  public Number visitFloatLiteral(final IRNode e) {	
	String floatStr = FloatLiteral.getToken( e );
	int length      = floatStr.length();
	char last       = floatStr.charAt( length-1 );
	if( last == 'F' || last == 'f' ) {	
		return Float.parseFloat(floatStr.substring(0, length-1));
	} else {
		return Double.parseDouble(floatStr);
	}
  }
  
  @Override
  public Boolean visitTrueExpression(final IRNode e) {   
    return Boolean.TRUE;
  }
  
  public Boolean visitFalseExpression(IRNode e) {
    return Boolean.FALSE;
  }
  
  @Override
  public Character visitCharLiteral(final IRNode e) {
	// TODO
	// includes the single quotes
	String value = CharLiteral.getToken(e);
    return Character.valueOf(value.charAt(1));
  }
  
  @Override
  public Object visitStringLiteral(final IRNode e) {
    /*
     * The Java Canonicalizer converts the NullLiteral to the StringLiteral
     * "null" if the NullLiteral is part of a StringConcat operation. We need to
     * check for this because the NullLiteral is never a constant expression.
     */
	String c = StringLiteral.getToken(e);
    if (JavaNode.wasImplicit(e) && c.equals(NULL_AS_STRING)) {
      return NO_CONST_VALUE;
    } else {
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
  }
  
  
  
  /*
   * Casts to primitive types and casts to type String (�15.16)
   */
  
  @Override
  public Object visitCastExpression(final IRNode e) {
    final IRNode type = CastExpression.getType(e);
    if (isPrimitiveTypeOrString(type)) {
      return doAccept(CastExpression.getExpr(e));
    } else {
      return NO_CONST_VALUE;
    }
  }



  private boolean isPrimitiveTypeOrString(final IRNode type) {
    return PrimitiveType.prototype.includes(type) ||
        (NamedType.prototype.includes(type) &&
            NamedType.getType(type).equals(JAVA_LANG_STRING));
  }
  
  
  
  /*
   * The unary operators +, -, ~, and ! (but not ++ or --) (�15.15.3, �15.15.4,
   * �15.15.5, �15.15.6)
   */
  
  @Override
  public Object visitPlusExpression(final IRNode e) {
    return doUnaryNumericPromotion(doAccept(PlusExpression.getOp(e)));
  }
  
  @Override
  public Object visitMinusExpression(final IRNode e) {
	Number value = doUnaryNumericPromotion(doAccept(MinusExpression.getOp(e)));
	if (value == null) {
		return NO_CONST_VALUE;
	}
	if (value instanceof Double) {
		return - ((Double) value).doubleValue();
	}
	if (value instanceof Long) {
		return - ((Long) value).longValue();
	}
	if (value instanceof Float) {
		return - ((Float) value).floatValue();
	}
	return -value.intValue();
  }
  
  @Override
  public Object visitComplementExpression(final IRNode e) {
	Number value = doUnaryNumericPromotion(doAccept(ComplementExpression.getOp(e)));
	if (value == null) {
		return NO_CONST_VALUE;
	}
	if (value instanceof Long) {
		return ~ ((Long) value).longValue();
	}
	return ~value.intValue();
  }
  
  @Override
  public Object visitNotExpression(final IRNode e) {
	Boolean value = (Boolean) doAccept(NotExpression.getOp(e));
	if (value == null) {
		return NO_CONST_VALUE;
	}
	return !value;
  }
  
  
  
  /*
   * The multiplicative operators *, /, and % (�15.17)
   */
  
  @Override
  public Object visitMulExpression(final IRNode e) {
	Number op1 = (Number) doAccept(MulExpression.getOp1(e));
	Number op2 = (Number) doAccept(MulExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() * op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() * op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() * op2.longValue();
	}
	return op1.intValue() * op2.intValue();
  }
  
  @Override
  public Object visitDivExpression(final IRNode e) {
	Number op1 = (Number) doAccept(DivExpression.getOp1(e));
	Number op2 = (Number) doAccept(DivExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() / op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() / op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() / op2.longValue();
	}
	return op1.intValue() / op2.intValue();	  
  }
  
  @Override
  public Object visitQualifiedName(final IRNode e) {
	final IRNode objectExpr = QualifiedName.getBase(e);
	final IBinding b = binder.getIBinding(objectExpr);
	
	// Is it a qualified name TypeName . Identifier 
	if (TypeDeclaration.prototype.includes(b.getNode())) {
		return checkForConstantField(e);
	}	
	return NO_CONST_VALUE;
  }
  
  @Override
  public Object visitRemExpression(final IRNode e) {
	Number op1 = (Number) doAccept(RemExpression.getOp1(e));
	Number op2 = (Number) doAccept(RemExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() % op2.longValue();
	}
	return op1.intValue() % op2.intValue();
  }
  
  
  
  /*
   * The additive operators + and - (�15.18)
   */
  
  @Override
  public Object visitAddExpression(final IRNode e) {
	Number op1 = (Number) doAccept(AddExpression.getOp1(e));
	Number op2 = (Number) doAccept(AddExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() + op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() + op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() + op2.longValue();
	}
	return op1.intValue() + op2.intValue();
  }
  
  @Override
  public Object visitSimpleName(final IRNode e) {
	  return checkForConstantField(e);
  }
  
  @Override
  public Object visitStringConcat(final IRNode e) {
	Object op1 = doAccept(StringConcat.getOp1(e));
	Object op2 = doAccept(StringConcat.getOp2(e));	  
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
    return op1.toString() + op2.toString();
  }
  
  @Override
  public Object visitSubExpression(final IRNode e) {
	Number op1 = (Number) doAccept(SubExpression.getOp1(e));
	Number op2 = (Number) doAccept(SubExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() - op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() - op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() - op2.longValue();
	}
	return op1.intValue() - op2.intValue();
  }
  
  
  
  /*
   * The shift operators <<, >>, and >>> (�15.19)
   */
  
  @Override
  public Object visitLeftShiftExpression(final IRNode e) {
	Number op1 = doUnaryNumericPromotion(doAccept(LeftShiftExpression.getOp1(e)));
	Number op2 = doUnaryNumericPromotion(doAccept(LeftShiftExpression.getOp2(e)));	  	
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Long) {
		if (op2 instanceof Long) {
			return op1.longValue() << op2.longValue();
		} else {
			return op1.longValue() << op2.intValue();
		}
	}
	if (op2 instanceof Long) {
		return op1.intValue() << op2.longValue();
	} else {
		return op1.intValue() << op2.intValue();
	}
  }
  
  @Override
  public Object visitRightShiftExpression(final IRNode e) {
	Number op1 = doUnaryNumericPromotion(doAccept(RightShiftExpression.getOp1(e)));
	Number op2 = doUnaryNumericPromotion(doAccept(RightShiftExpression.getOp2(e)));	  	
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Long) {
		if (op2 instanceof Long) {
			return op1.longValue() >> op2.longValue();
		} else {
			return op1.longValue() >> op2.intValue();
		}
	}
	if (op2 instanceof Long) {
		return op1.intValue() >> op2.longValue();
	} else {
		return op1.intValue() >> op2.intValue();
	}	  
  }
  
  @Override
  public Object visitUnsignedRightShiftExpression(final IRNode e) {
	Number op1 = doUnaryNumericPromotion(doAccept(UnsignedRightShiftExpression.getOp1(e)));
	Number op2 = doUnaryNumericPromotion(doAccept(UnsignedRightShiftExpression.getOp2(e)));	  	
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Long) {
		if (op2 instanceof Long) {
			return op1.longValue() >>> op2.longValue();
		} else {
			return op1.longValue() >>> op2.intValue();
		}
	}
	if (op2 instanceof Long) {
		return op1.intValue() >>> op2.longValue();
	} else {
		return op1.intValue() >>> op2.intValue();
	}
  }
  
  
  
  /*
   * The relational operators <, <=, >, and >= (but not instanceof) (�15.20)
   */
  
  @Override
  public Object visitLessThanExpression(final IRNode e) {
	Number op1 = (Number) doAccept(LessThanExpression.getOp1(e));
	Number op2 = (Number) doAccept(LessThanExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() < op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() < op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() < op2.longValue();
	}
	return op1.intValue() < op2.intValue();
  }
  
  @Override
  public Object visitLessThanEqualExpression(final IRNode e) {
	Number op1 = (Number) doAccept(LessThanEqualExpression.getOp1(e));
	Number op2 = (Number) doAccept(LessThanEqualExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() <= op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() <= op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() <= op2.longValue();
	}
	return op1.intValue() <= op2.intValue();	  
  }
  
  @Override
  public Object visitGreaterThanExpression(final IRNode e) {
	Number op1 = (Number) doAccept(GreaterThanExpression.getOp1(e));
	Number op2 = (Number) doAccept(GreaterThanExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() > op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() > op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() > op2.longValue();
	}
	return op1.intValue() > op2.intValue();
  }
  
  @Override
  public Object visitGreaterThanEqualExpression(final IRNode e) {
	Number op1 = (Number) doAccept(GreaterThanEqualExpression.getOp1(e));
	Number op2 = (Number) doAccept(GreaterThanEqualExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Double || op2 instanceof Double) {
		return op1.doubleValue() >= op2.doubleValue();
	}
	if (op1 instanceof Float || op2 instanceof Float) {
		return op1.floatValue() >= op2.floatValue();
	}
	if (op1 instanceof Long || op2 instanceof Long) {
		return op1.longValue() >= op2.longValue();
	}
	return op1.intValue() >= op2.intValue();	
  }
  
  
  
  /*
   * The equality operators == and != (�15.21)
   */
  
  @Override
  public Object visitEqExpression(final IRNode e) {
	  Object op1 = doAccept(EqExpression.getOp1(e));
	  Object op2 = doAccept(EqExpression.getOp2(e));
	  if (op1 == null || op2 == null) {
		  return NO_CONST_VALUE;
	  }
	  return op1.equals(op2); // TODO is this right w/ promotion?
  }
  
  @Override
  public Object visitNotEqExpression(final IRNode e) {
	  Object op1 = doAccept(NotEqExpression.getOp1(e));
	  Object op2 = doAccept(NotEqExpression.getOp2(e));
	  if (op1 == null || op2 == null) {
		  return NO_CONST_VALUE;
	  }
	  return !op1.equals(op2); // TODO is this right w/ promotion?
  }



  /*
   * The bitwise and logical operators &, ^, and | (�15.22)
   */
  
  @Override
  public Object visitAndExpression(final IRNode e) {
	Object op1 = doAccept(AndExpression.getOp1(e));
	Object op2 = doAccept(AndExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Boolean) { // Assume op2 is boolean also
		return (Boolean) op1 & (Boolean) op2;
	}
	Number n1 = (Number) op1;
	Number n2 = (Number) op2;
	if (op1 instanceof Long || op2 instanceof Long) {
		return n1.longValue() & n2.longValue();
	}
	return n1.intValue() & n2.intValue();
  }
  
  @Override
  public Object visitXorExpression(final IRNode e) {
	Object op1 = doAccept(XorExpression.getOp1(e));
	Object op2 = doAccept(XorExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Boolean) { // Assume op2 is boolean also
		return (Boolean) op1 ^ (Boolean) op2;
	}
	Number n1 = (Number) op1;
	Number n2 = (Number) op2;
	if (op1 instanceof Long || op2 instanceof Long) {
		return n1.longValue() ^ n2.longValue();
	}
	return n1.intValue() ^ n2.intValue();	  
  }
  
  @Override
  public Object visitOrExpression(final IRNode e) {
	Object op1 = doAccept(OrExpression.getOp1(e));
	Object op2 = doAccept(OrExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
	if (op1 instanceof Boolean) { // Assume op2 is boolean also
		return (Boolean) op1 | (Boolean) op2;
	}
	Number n1 = (Number) op1;
	Number n2 = (Number) op2;
	if (op1 instanceof Long || op2 instanceof Long) {
		return n1.longValue() | n2.longValue();
	}
	return n1.intValue() | n2.intValue();	
  }
  
  
  
  /*
   * The conditional-and operator && and the conditional-or operator || (�15.23,
   * �15.24)
   */
  
  @Override
  public Object visitConditionalAndExpression(final IRNode e) {
	Boolean op1 = (Boolean) doAccept(ConditionalAndExpression.getOp1(e));
	Boolean op2 = (Boolean) doAccept(ConditionalAndExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
    return op1 && op2; 
  }
  
  @Override
  public Object visitConditionalOrExpression(final IRNode e) {
	Boolean op1 = (Boolean) doAccept(ConditionalOrExpression.getOp1(e));
	Boolean op2 = (Boolean) doAccept(ConditionalOrExpression.getOp2(e));
	if (op1 == null || op2 == null) {
		return NO_CONST_VALUE;
	}
    return op1 || op2; 
  }
  
  
  
  /*
   * The ternary conditional operator ? : (�15.25)
   */
  
  @Override
  public Object visitConditionalExpression(final IRNode e) {
	Boolean cond = (Boolean) doAccept(ConditionalExpression.getCond(e));
	if (cond == null) {
		return NO_CONST_VALUE;
	}
	Object ifTrue = doAccept(ConditionalExpression.getIftrue(e));
	Object ifFalse = doAccept(ConditionalExpression.getIffalse(e));
    if (ifTrue == null || ifFalse == null) {
    	return NO_CONST_VALUE;
    }
    return cond ? ifTrue : ifFalse;   
  }
  
  
  
  /*
   * Parenthesized expressions (�15.8.5) whose contained expression is a
   * constant expression.
   */
  
  @Override
  public Object visitParenExpression(final IRNode e) {
    return doAccept(ParenExpression.getOp(e));
  }
  
  
  
  /*
   * Simple names (�6.5.6.1) that refer to constant variables (�4.12.4).
   * 
   * Qualified names (�6.5.6.2) of the form TypeName . Identifier that refer to
   * constant variables (�4.12.4).
   * 
   * 
   * A variable of primitive type or type String, that is final and initialized
   * with a compile-time constant expression (�15.28), is called a constant
   * variable.
   */
  @Override
  public Object visitFieldRef(final IRNode e) {
    final IRNode objectExpr = FieldRef.getObject(e);
    
    // Is it a simple name or a qualified name TypeName . Identifier 
    if ((ThisExpression.prototype.includes(objectExpr) && 
        JavaNode.wasImplicit(objectExpr)) ||
        TypeExpression.prototype.includes(objectExpr)) {
    	return checkForConstantField(e);
    }
    return NO_CONST_VALUE;
  }
    
  private Object checkForConstantField(final IRNode e) {
	// Check the field declaration
	final IRNode fdecl = binder.getBinding(e);
	if (EnumConstantDeclaration.prototype.includes(fdecl)) {
		// TODO actually considered a const, but how to get the actual value?
		return EnumConstantDeclaration.getId(fdecl);
	}
	if (TypeUtil.isJavaFinal(fdecl)) {
		// final, now check the initializer
		final IRNode init = VariableDeclarator.getInit(fdecl);
		if (Initialization.prototype.includes(init)) {
			// (1) Check the type of the field: must be primitive or String
			// (2) check if the initializer is constant
			return isPrimitiveTypeOrString(VariableDeclarator.getType(fdecl)) ?
					doAccept(Initialization.getValue(init)) : NO_CONST_VALUE;
		}
	}
    return NO_CONST_VALUE;
  }  
  
  /*
   * Box and Unbox expressions do not exist in the Java syntax, they are
   * introduced by our Java Canonicalizer.  We pass through them here
   * because they aren't part of the syntax.  I think we only encounter them
   * in a Constant Expression when it is a String concatenation such as
   * "foo" + 3, where the canonicalizer turns the 3 into X.toString(), where
   * X is a box expression of the literal 3.
   */
  
  @Override
  public Object visitBoxExpression(final IRNode e) {
    return doAccept(BoxExpression.getOp(e));
  }
  
  @Override
  public Object visitUnboxExpression(final IRNode e) {
    return doAccept(UnboxExpression.getOp(e));
  }
  
  @Override
  public Object visitNameExpression(final IRNode e) {
	return doAccept(NameExpression.getName(e));
  }
  
  
  /*
   * As stated above, the Java canonicalizer introduces calls to toString() 
   * in StringConcat expressions.  Such calls are modeled as
   * NonPolymorphicMethodCall nodes that have an arg list that is marked
   * as implicit.  We want to pass through them and check the status of the
   * object expression.
   */
  
  @Override
  public Object visitNonPolymorphicMethodCall(final IRNode e) {
    if (NonPolymorphicMethodCall.getMethod(e).equals(TO_STRING) &&
        JavaNode.wasImplicit(NonPolymorphicMethodCall.getArgs(e))) {
      return doAccept(NonPolymorphicMethodCall.getObject(e));
    } else {
      return NO_CONST_VALUE;
    }
  }
  
  /**
   * Unary numeric promotion is performed on expressions in the following situations:
   * Each dimension expression in an array creation expression (15.10)
   * The index expression in an array access expression (15.13)
   * The operand of a unary plus operator + (15.15.3)
   * The operand of a unary minus operator - (15.15.4)
   * The operand of a bitwise complement operator ~ (15.15.5)
   * Each operand, separately, of a shift operator >>, >>>, or << (15.19); 
   * therefore a long shift distance (right operand) does not promote the value 
   * being shifted (left operand) to long 
   */
  private Number doUnaryNumericPromotion(Object orig) {
	 if (orig == null) {
		 return null;
	 }
	 if (orig instanceof Byte) {
		 return Integer.valueOf(((Byte) orig).intValue());
	 } 
	 else if (orig instanceof Short) {	 
		 return Integer.valueOf(((Short) orig).intValue());
	 }
	 else if (orig instanceof Character) {	 
		 return Integer.valueOf(((Character) orig).charValue());
	 }
	 return (Number) orig;
  }
}
