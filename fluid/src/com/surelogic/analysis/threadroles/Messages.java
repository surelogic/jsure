package com.surelogic.analysis.threadroles;

import com.surelogic.dropsea.ir.Category;

/**
 * This class is probably following some old way of doing things.  I'm not
 * sure why this color roles are separated from the items in
 * {@link com.surelogic.analysis.colors.ColorMessages}
 */
public final class Messages {
  // Prevent instantiation
  private Messages() {
    super();
  }

  public static final Category assuranceCategory = Category
  .getResultInstance("Thread role assurances");  
  
  public static final String ColorSecondPass_inferredColor = "Inferred @ThreadRole {0} for {1}";
  public static final String ColorSecondPass_inheritedColor = "Inherited @ThreadRole {0} for {1}";
  public static final String ColorSecondPass_inheritedTransparent = "Inherited @ThreadRoleTransparent for {0}";
  public static final String ColorSecondPass_colorContextDrop = "{0} is accessed from ThreadRole context {1}";
}
