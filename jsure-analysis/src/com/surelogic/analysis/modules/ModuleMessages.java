/*
 * Created on Oct 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.modules;

import java.util.ResourceBundle;

import edu.cmu.cs.fluid.util.AbstractMessages;


public class ModuleMessages extends AbstractMessages {
  private static final String BUNDLE_NAME = "edu.cmu.cs.fluid.java.analysis.ModuleAnalysisMessages"; //$NON-NLS-1$

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
      .getBundle(BUNDLE_NAME);

  private ModuleMessages() {
  }

  public static String getString(String key) {
    return getString(RESOURCE_BUNDLE, key);
  }
}
