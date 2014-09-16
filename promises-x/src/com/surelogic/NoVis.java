package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated Java entity is not exported from its home
 * module.
 * 
 * @see Vis
 * 
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface NoVis {
  // marker annotation
}
