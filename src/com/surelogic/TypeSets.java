package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link TypeSet} annotations.  It is an error
 * for an entity to have both a {@code TypeSets} and a {@code TypeSet} annotation.
 * 
 * @see TypeSet
 */
@Documented
@Target(ElementType.PACKAGE)
public @interface TypeSets {
  /**
   * The {@link TypeSet} annotations to apply to the package.
   */
  TypeSet[] value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
