/**
 * 
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author dfsuther
 * Import ThreadRole declarations and ThreadRoleRenames from a package or type
 * into the annotated scope. The package or type name is specified using the 
 * syntax of Java {@code import} clauses. Note, however, that the {@code package.name.*;}
 * form causes import from the {@code package.name.package-info.java} file rather 
 * than importing from all the individual types contained within {@code package.name}.
 */
@Documented
@Target( { ElementType.PACKAGE, ElementType.TYPE })
public @interface ThreadRoleImport {
	/**
	 * @return the specification of the type or package to import from. This is
	 * either a TypeName or a PackageOrTypeName.* as appropriate.
	 */
	String value();
	  
	  /**
	   * When {@code true}, indicates that this annotation has priority over any
	   * annotations that apply to the same node that originate from scoped promises.
	   */
	  boolean override() default true;
}
