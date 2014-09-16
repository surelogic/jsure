package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Two alternatives:
 * 
 * <pre>
 *     1: @Module("modName")
 *     2: @Module("modName") for &lt;locationSpec&gt;
 * </pre>
 * 
 * Alternative 1 states that the annotated Type or CU is contained within the
 * Fluid module system Module modName. Alternative 2 serves as a scoped promise,
 * placing the annotation {@code @Module("modName")} at the specified locations.
 * The locationSpec is identical to that used for the {@link Promise} annotation
 * (in general), but must name only types. Note: The current implementation
 * supports only {@code *} for location specs, and places the annotation at all
 * types in scope. All other location specs are currently rejected.
 * 
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface Module {

  /**
   * The value as described above.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
