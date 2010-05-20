/**
 * 
 */
package com.surelogic;

import java.lang.annotation.Documented;

/**
 * @author dfsuther
 *
 */
@Documented
public @interface ThreadRoleGrant {
	
	  /**
	 * A comma-separated list of thread role names.
	 */
	String value();
	  
	  /**
	   * When {@code true}, indicates that this annotation has priority over any
	   * annotations that apply to the same node that originate from scoped promises.
	   */
	  boolean override() default true;

}
