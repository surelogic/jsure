package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declare a lexical shorthand name for a ThreadRole expression. the declared
 * name is replaced by the expression in all ThreadRole annotations located in
 * scopes that have visibility to the ThreadRoleRename annotation
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface ThreadRoleRename {
  /**
   * Syntax is {@code <shortname> renames <ThreadRoleExpression>} The shortname
   * is any lexically legal ThreadRole name; the expression is a boolean
   * expression over ThreadRoleNames. The current implementation accepts only
   * expressions in Disjunctive Normal Form.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
