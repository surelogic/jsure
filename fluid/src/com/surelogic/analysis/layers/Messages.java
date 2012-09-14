package com.surelogic.analysis.layers;

import com.surelogic.dropsea.ir.Category;

public final class Messages {
  private Messages() {
    // private constructor to prevent instantiation
  }

  public static final Category DSC_LAYERS_ISSUES = Category.getInstance(350);

  public static final int PROHIBITED_REFERENCE = 350;
  public static final int ALL_TYPES_PERMITTED = 351;
  public static final int PERMITTED_REFERENCE = 352;
  public static final int CYCLE = 353;
  public static final int TYPE_INVOLVED = 354;
  public static final int TYPESET_INVOLVED = 355;
}
