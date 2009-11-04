package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author dfsuther
 * 
 * Establishes a constraint on the colors of threads that may execute the annotated method.
 * At analysis time the "Thread Coloring" analysis will enforce the constraint that the
 * calling thread must always have a set of associated color bindings that satisfy the 
 * boolean expression.
 * <p>
 * Note that {@code Color true} is equivalent to the {@code Transparent} annotation.
 * 
 * @see Transparent
 *
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Color {
  /**
   * A Boolean expression over thread color names, in Disjunctive Normal Form. The
   * expression establishes the maximum acceptable color environment for the annotated
   * method or constructor. 
   * The value of this attribute must conform to the following grammar (in 
   * <a href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * <pre>
   * value = colorExpr
   * 
   * colorNotExpr = "!" colorName
   * 
   * colorOrExpr = colorOrElem +("|" colorOrElem)
   * 
   * colorOrElem = colorAndElem / colorAndParen
   * 
   * colorAndExpr = colorAndElem "&" colorAndElem
   * 
   * colorAndElem = colorName / colorNotExpr
   * 
   * colorAndParen = "(" colorAndExpr ")"
   * 
   * colorExpr = colorName / colorAndExpr / colorOrExpr / colorNotExpr / colorParenExpr
   * 
   * colorParenExpr = "(" colorExpr ")"
   * 
   * colorName = simpleColorName / qualifiedColorName
   *  
   * qualifiedColorName = IDENTIFIER *("." IDENTIFIER)
   * 
   * simpleColorName = IDENTIFIER
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   */
  String value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
