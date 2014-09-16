package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link ThreadRoleImport} annotations. It is
 * a modeling error for a class to have both a {@link ThreadRoleImports} and a
 * {@link ThreadRoleImport} annotation.
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface ThreadRoleImports {
  /**
   * The {@link ThreadRoleImport} annotations to apply to the class.
   * 
   * @return the {@link ThreadRoleImport} annotations to apply to the class.
   */
  ThreadRoleImport[] value();
}
