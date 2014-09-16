package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link Module} annotations. It is a
 * modeling error for a package, type, method, or constructor to have both a
 * {@link Modules} and a {@link Module} annotation.
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface Modules {
  /**
   * The {@link Module} annotations to apply to the class or method.
   * 
   * @return the {@link Module} annotations to apply to the class or method.
   */
  Module[] value();
}
