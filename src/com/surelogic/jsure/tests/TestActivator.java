/*
 * Created on May 4, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.jsure.tests;

import org.eclipse.core.runtime.Plugin;

public class TestActivator extends Plugin {
  private static TestActivator instance;

  public TestActivator() {
    instance = this;
  }
  
  static TestActivator getDefault() {
    return instance;
  }
}
