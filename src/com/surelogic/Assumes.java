package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link Assume} annotations.  It is an error
 * for an entity to have both an {@code Assumes} and an {@code Assume} annotation.
 * 
 * @see Assume
 * @see Promise
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR,
  ElementType.METHOD, ElementType.FIELD})
public @interface Assumes {
  /**
   * The {@link Assume} annotations to apply to the class.
   */
  Assume[] value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
