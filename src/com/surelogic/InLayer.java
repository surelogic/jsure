package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares that the annotated type is part of the named layers.
 * 
 * @see Layer
 */
@Documented
@Target(ElementType.TYPE)
public @interface InLayer {
  /**
   * The one or more layers that the type is part of. The
   * attribute is restricted to strings that match the following grammar:
   * 
   * <p>
   * value = layer_spec *("<tt>,</tt>" layer_spec)
   * 
   * <p>
   * layer_spec = dotted_name <i>; Layer name</i><br>
   * layer_spec /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("<tt>,</tt>" name) "<tt>}</tt>" <i>; Enumeration of layers</i>
   * 
   * <p>The braces "<tt>{</tt>" "<tt>}</tt>" are syntactic sugar
   * used to enumerate a list of layers that share the same prefix.
   */
  String value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
