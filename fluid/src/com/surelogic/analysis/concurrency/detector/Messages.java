package com.surelogic.analysis.concurrency.detector;

import java.util.*;

import edu.cmu.cs.fluid.sea.Category;

final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_THREAD_CREATION = Category.getInstance(50);
  public static final Category DSC_RUNNABLE_CREATION = Category.getInstance(51);
  public static final Category DSC_THREAD_STARTS = Category.getInstance(52);

  
  
  public static final int INSTANCE_CREATED = 50;
  public static final int THREAD_STARTED = 51;
  
  
  
  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
  static {
    code2name.put(INSTANCE_CREATED, "concurrencyDetector");
    code2name.put(THREAD_STARTED, "concurrencyDetector");
  }
  
  public static String toString(int code) {
	  return code2name.get(code);
  }
}
