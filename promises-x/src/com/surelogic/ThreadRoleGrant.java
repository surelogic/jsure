package com.surelogic;

import java.lang.annotation.Documented;

/**
 * Grants thread roles.
 */
@Documented
public @interface ThreadRoleGrant {

  /**
   * A comma-separated list of thread role names.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
