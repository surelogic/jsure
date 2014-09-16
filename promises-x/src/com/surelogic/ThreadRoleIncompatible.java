package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declare that the named ThreadRoles are "incompatible" -- that is, at most one
 * of the named roles may be simultaneously present in a thread role
 * environment. The dynamic view of this property is that no thread may be
 * associated with more than one of the named roles at any given instant during
 * execution.
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface ThreadRoleIncompatible {
  /**
   * On types and packages, this is a comma-separated list of role names.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
