package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link Layer} annotations.  It is an error
 * for an entity to have both a {@code Layers} and a {@code Layer} annotation.
 * 
 * @see Layer
 */
@Documented
@Target(ElementType.PACKAGE)
public @interface Layers {
  /**
   * The {@link Layer} annotations to apply to the package.
   */
  Layer[] value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
