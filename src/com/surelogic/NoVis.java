package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author dfsuther
 * 
 *         Indicates that the annotated Java entity is not exported from
 *         its home module.
 * 
 * @see Vis
 * 
 */
@Documented
@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR,
    ElementType.TYPE })
public @interface NoVis {
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped
   * promises.
   */
  boolean override() default true;

}
