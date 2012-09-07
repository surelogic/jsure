package com.surelogic.analysis.testing;

import edu.cmu.cs.fluid.sea.Category;

final class Messages {
  private Messages() {
    // private constructor to prevent instantiation
  }

  public static final Category DSC_BCA = Category.getInstance(500);
  public static final Category DSC_COLLECT_METHOD_CALLS = Category.getInstance(510);
  public static final Category DSC_LOCAL_VARIABLES = Category.getInstance(520);
  public static final Category DSC_NON_NULL = Category.getInstance(530);
  public static final Category DSC_TEST_ALIAS = Category.getInstance(540);

  public static final int BINDS_TO = 500;

  public static final int CALLS = 510;

  public static final int LOCAL_VARS = 520;

  public static final int NOT_NULL = 530;
  public static final int MAYBE_NULL = 531;
  public static final int RAWNESS = 532;
  public static final int NOT_ASSIGNED = 533;

  public static final int ALIASED_PARAMETERS = 540;

}
