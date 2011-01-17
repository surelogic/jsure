package com.surelogic.analysis.layers;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.sea.Category;

public final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }


  
  public static final Category DSC_LAYERS_ISSUES = Category.getInstance2(350);
  

  
  public static final int PROHIBITED_REFERENCE = 350;
  public static final int ALL_TYPES_PERMITTED = 351;
  public static final int PERMITTED_REFERENCE = 352;
  public static final int CYCLE = 353;
  public static final int TYPE_INVOLVED = 354;
  public static final int TYPESET_INVOLVED = 355; 
  
  
  
  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
  static {
    code2name.put(PROHIBITED_REFERENCE, "JSure");
    code2name.put(ALL_TYPES_PERMITTED, "JSure");
    code2name.put(PERMITTED_REFERENCE, "JSure");
    code2name.put(CYCLE, "JSure");
    code2name.put(TYPE_INVOLVED, "JSure");
    code2name.put(TYPESET_INVOLVED, "JSure");
  }
  
  public static String toString(int code) {
	  return code2name.get(code);
  }
}
