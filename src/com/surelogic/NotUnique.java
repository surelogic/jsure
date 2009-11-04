package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that a parameter is not required to be unique. This annotation is
 * intended to be used only when the parameter is part of an overriding method
 * implementation and the original method requires the parameter to be unique,
 * but the overriding implementation does not require the parameter to be
 * unique:
 * 
 * <pre>
 *   class C {
 *     public void method m(@Unique Object p) { ... }
 *   }
 *   
 *   class D extends C {
 *     public void method m(@NotUnique Object p) { ... }
 *   }
 * </pre>
 * 
 * <p>
 * In this case, if {@code D.m(Object)} did not annotate parameter {@code p}
 * then annotation inheritance would still ensure that {@code p} is unique.
 * <p>
 * In the case of a unique receiver, the annotation is placed on the method:
 * 
 * <pre>
 *   class C {
 *     &#064;Unique(&quot;this&quot;)
 *     public void method m() { ... }
 *   }
 *   
 *   class D extends C {
 *     &#064;NotUnique(&quot;this&quot;)
 *     public void method m() { ... }
 *   }
 * </pre>
 * <p>
 * It is an error to annotate a parameter if the parameter's type is primitive.
 * 
 * @see Unique
 */
@Documented
@Target( { ElementType.PARAMETER, ElementType.METHOD })
public @interface NotUnique {
	/**
	 * When annotating a method, this must be <code>"this"</code> to indicated
	 * it is the receiver that it is being declared as not unique. Otherwise it
	 * must be the empty string.
	 */
	String value() default "";

	/**
	 * When {@code true}, indicates that this annotation has priority over any
	 * annotations that apply to the same node that originate from scoped
	 * promises.
	 */
	boolean override() default true;
}
