package com.surelogic.analysis.concurrency.detector;

import com.surelogic.dropsea.ir.Category;

final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }
  public static final Category DSC_THREAD_CREATION = Category.getInstance(50);
  public static final Category DSC_RUNNABLE_CREATION = Category.getInstance(51);
  public static final Category DSC_THREAD_STARTS = Category.getInstance(52);

  
  
  public static final int INSTANCE_CREATED = 50;
  public static final int THREAD_STARTED = 51;
}
