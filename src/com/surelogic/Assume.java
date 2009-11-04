package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares an assumption about a portion of the system. <em>Say more here.</em>
 * 
 * <p>To declare
 * more than one assumption for an entity use the {@link Assumes} annotation.  It is an error
 * for an entity to have both an {@code Assumes} and an {@code Assume} annotation.
 * 
 * @see Assumes
 * @see Promise
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR,
  ElementType.METHOD, ElementType.FIELD})
public @interface Assume {
  /**
   * The value of the attribute must
   * conform to the following grammar (in ABNF):
   * <pre>
   * TODO: Wait until the grammar is updated
   * </pre>
   */
  String value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
