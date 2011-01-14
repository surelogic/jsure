package com.surelogic.analysis.threads;

import java.util.HashMap;
import java.util.Map;

public final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }
  
  
  
  public static final int NO_THREADS_STARTED = 1;
  public static final int PROHIBITED = 2;
  public static final int CALLED_METHOD_DOES_PROMISE = 3;
  public static final int CALLED_METHOD_DOES_NOT_PROMISE = 4;
  
  
  
  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
  static {
    code2name.put(NO_THREADS_STARTED, "ThreadEffectsAnalysis_noThreadsDrop");
    code2name.put(PROHIBITED, "ThreadEffectsAnalysis_threadEffectDrop");
    code2name.put(CALLED_METHOD_DOES_PROMISE, "ThreadEffectsAnalysis_callPromiseDrop");
    code2name.put(CALLED_METHOD_DOES_NOT_PROMISE, "ThreadEffectsAnalysis_callNotPromiseDrop");
  }
  
  public static String toString(int code) {
	  return code2name.get(code);
  }
}
