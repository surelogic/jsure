package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Notes the module visibility of the annotated method, class or field.
 * 
 * @see Module
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface Vis {
  /**
   * This attribute names the module that is the highest level of the module
   * hierarchy from which the annotated Java entity is exported. Its value
   * should be either an empty string (thus indicating the most closely
   * enclosing module) or the name of a module that is an ancestor of the
   * current module.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
