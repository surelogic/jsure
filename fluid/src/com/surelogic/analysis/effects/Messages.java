package com.surelogic.analysis.effects;

import java.util.*;

import edu.cmu.cs.fluid.sea.Category;

public final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_EFFECTS_IN_CLASS_INIT = Category.getInstance(150); 
  
  public static final int UNACCOUNTED_FOR = 150;
  public static final int CHECKED_BY = 151;
  public static final int EMPTY_EFFECTS = 152;
  public static final int CONSTRUCTOR_RULE = 153;
  public static final int PARAMETER_EVIDENCE = 154;
  public static final int CLASS_INIT_EFFECT = 156;
  public static final int REASON_NO_DECLARED_EFFECT = 157;
  public static final int REASON_RECEIVER_IS_IMMUTABLE = 158;
  public static final int READONLY_REFERENCE = 159;
  public static final int REASON_FINAL_FIELD = 160;
  public static final int BCA_EVIDENCE = 161;
  public static final int AGGREGATION_EVIDENCE = 162;
  public static final int ACE_EVIDENCE = 163;
  public static final int QRCVR_CONVERSION_EVIDENCE = 164;
  public static final int UNKNOWN_REF_CONVERSION_EVIDENCE = 165;
  public static final int REASON_NULL_REFERENCE = 166;
  public static final int REASON_NEW_OBJECT = 167;
  public static final int REASON_UNIQUE_RETURN = 168;
  public static final int ITERATOR_EFFECTS_CONVERSION = 169;
  
  
  
  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
  static {
	  code2name.put(UNACCOUNTED_FOR, "EffectAssurance_msgUnaccountedFor");
	  code2name.put(CHECKED_BY, "EffectAssurance_msgCheckedBy");
	  code2name.put(EMPTY_EFFECTS, "EffectAssurance_msgEmptyEffects");
	  code2name.put(CONSTRUCTOR_RULE, "EffectAssurance_msgContructorRule");
	  code2name.put(PARAMETER_EVIDENCE, "EffectAssurance");
	  code2name.put(CLASS_INIT_EFFECT, "EffectAssurance");
    code2name.put(REASON_NO_DECLARED_EFFECT, "EffectAssurance");
    code2name.put(REASON_RECEIVER_IS_IMMUTABLE, "EffectAssurance");
    code2name.put(READONLY_REFERENCE, "EffectAssurance");
    code2name.put(REASON_FINAL_FIELD, "EffectAssurance");
    code2name.put(BCA_EVIDENCE, "EffectAssurance");
    code2name.put(AGGREGATION_EVIDENCE, "EffectAssurance");
    code2name.put(ACE_EVIDENCE, "EffectAssurance");
    code2name.put(QRCVR_CONVERSION_EVIDENCE, "EffectAssurance");
    code2name.put(UNKNOWN_REF_CONVERSION_EVIDENCE, "EffectAssurance");
    code2name.put(REASON_NULL_REFERENCE, "EffectAssurance");
    code2name.put(REASON_NEW_OBJECT, "EffectAssurance");
    code2name.put(ITERATOR_EFFECTS_CONVERSION, "EffectsAssurance");
  }
  
  public static String toString(int code) {
	  return code2name.get(code);
  }
}
