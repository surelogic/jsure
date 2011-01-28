/*
 * Created on May 4, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.jsure.tests;

import org.eclipse.core.runtime.Plugin;

public class Activator extends Plugin {
  private static Activator instance;

  public Activator() {
    instance = this;
  }
  
  static Activator getDefault() {
    return instance;
  }
}
