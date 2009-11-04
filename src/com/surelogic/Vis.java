package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Notes the module visibility of the annotated method, class or field.
 * 
 * @see Transparent
 */
@Documented
@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR,
		ElementType.TYPE })
public @interface Vis {
	/**
	 * This attribute names the module that is the highest level of the module
	 * hierarchy from which the annotated Java entity is exported. Its value
	 * should be either an empty string (thus indicating the most closely
	 * enclosing module) or the name of a module that is an ancestor of the
	 * current module.
	 */
	String value();

	/**
	 * When {@code true}, indicates that this annotation has priority over any
	 * annotations that apply to the same node that originate from scoped
	 * promises.
	 */
	boolean override() default true;

}
