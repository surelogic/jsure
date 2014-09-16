package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link ThreadRole} annotations. It is a
 * modeling error for a package, type, method, or constructor to have both a
 * {@link ThreadRoles} and a {@link ThreadRole} annotation.
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface ThreadRoles {
  /**
   * The {@link ThreadRole} annotations to apply to the class or method.
   * 
   * @return the {@link ThreadRole} annotations to apply to the class or method.
   */
  ThreadRole[] value();
}
