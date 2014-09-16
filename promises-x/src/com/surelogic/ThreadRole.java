package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * When applied to a method or constructor: Establishes a constraint on the
 * roles of threads that may execute the annotated method. At analysis time the
 * "Thread Role" analysis will enforce the constraint that the calling thread
 * must always have a set of associated ThreadRole bindings that satisfy the
 * boolean expression.
 * <p>
 * Note that {@code ThreadRole true} is equivalent to the
 * {@code ThreadRoleTransparent} annotation.
 * 
 * See also {@link ThreadRoleTransparent}
 * <p>
 * When applied to a type, or when placed in a package.java file or a
 * module-scoped location: Declares a non-empty list of Thread roles. The
 * declared roles can be referred to in other thread role annotations using the
 * qualified name of the declared role, or using their short name (if it is
 * visible in scope). Use the {@code ThreadRoleImport} annotation to import
 * these declarations into a scope.
 * 
 * See also {@link ThreadRoleImport}
 * 
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface ThreadRole {
  /**
   * On methods and constructors: A Boolean expression over thread role names,
   * in Disjunctive Normal Form. The expression establishes the maximum
   * acceptable role environment for the annotated method or constructor. The
   * value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = roleExpr / roleNameList
   * 
   * roleNameList = simpleRoleName +(&quot;,&quot; simpleRoleName)
   * 
   * roleNotExpr = "!" roleName
   * 
   * roleOrExpr = roleOrElem +("|" roleOrElem)
   * 
   * roleOrElem = roleAndElem / roleAndParen
   * 
   * roleAndExpr = roleAndElem "&amp;" roleAndElem
   * 
   * roleAndElem = roleName / roleNotExpr
   * 
   * roleAndParen = "(" roleAndExpr ")"
   * 
   * roleExpr = roleName / roleAndExpr / roleOrExpr / roleNotExpr / roleParenExpr
   * 
   * roleParenExpr = "(" roleExpr ")"
   * 
   * roleName = simpleRoleName / qualifiedRoleName
   *  
   * qualifiedRoleName = IDENTIFIER *("." IDENTIFIER)
   * 
   * simpleRoleName = IDENTIFIER
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * On types and packages, this is a simple list of role names.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
