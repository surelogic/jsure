package com.surelogic.analysis.effects;

import java.util.*;

import edu.cmu.cs.fluid.sea.Category;

final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_EFFECTS_IN_CLASS_INIT = Category.getInstance2(150); 
  
  public static final int UNACCOUNTED_FOR = 150;
  public static final int CHECKED_BY = 151;
  public static final int EMPTY_EFFECTS = 152;
  public static final int CONSTRUCTOR_RULE = 153;
  public static final int PARAMETER_EVIDENCE = 154;
  public static final int NO_EFFECTS = 155;
  public static final int CLASS_INIT_EFFECT = 156;
  
  
  
  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
  static {
	  code2name.put(UNACCOUNTED_FOR, "EffectAssurance_msgUnaccountedFor");
	  code2name.put(CHECKED_BY, "EffectAssurance_msgCheckedBy");
	  code2name.put(EMPTY_EFFECTS, "EffectAssurance_msgEmptyEffects");
	  code2name.put(CONSTRUCTOR_RULE, "EffectAssurance_msgContructorRule");
	  code2name.put(PARAMETER_EVIDENCE, "EffectAssurance");
	  code2name.put(NO_EFFECTS, "EffectAssurance");
	  code2name.put(CLASS_INIT_EFFECT, "EffectAssurance");
  }
  
  public static String toString(int code) {
	  return code2name.get(code);
  }
}
