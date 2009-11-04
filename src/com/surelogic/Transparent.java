package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method may be called from any thread color context.
 * Semantically equivalent to {@code @Color true}.
 * @see Color
 * @author dfsuther

 *
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Transparent {
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
