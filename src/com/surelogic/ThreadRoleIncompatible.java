package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author dfsuther
 * Declare that the named ThreadRoles are "incompatible" -- that is, at most one
 * of the named roles may be simultaneously present in a thread role environment.
 * The dynamic view of this property is that no thread may be associated with 
 * more than one of the named roles at any given instant during execution.
 * 
 */
@Documented
@Target( { ElementType.PACKAGE, ElementType.TYPE })
public @interface ThreadRoleIncompatible {
	/**
	 * On types and packages, this is a comma-separated list of role names.
	 */
	String value();

	/**
	 * When {@code true}, indicates that this annotation has priority over any
	 * annotations that apply to the same node that originate from scoped
	 * promises.
	 */
	boolean override() default true;
}
