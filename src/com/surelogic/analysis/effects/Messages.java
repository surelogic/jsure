package com.surelogic.analysis.effects;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages{
  private static final String BUNDLE_NAME = "com.surelogic.analysis.effects.messages"; //$NON-NLS-1$

  public static final String EffectAssurance_msgUnaccountedFor = "\"{0}\" not accounted for by declared effect(s)";

  public static final String EffectAssurance_msgCheckedBy = "\"{0}\" checked by \"{1}\"";

  public static final String EffectAssurance_msgEmptyEffects = "Empty effects trivially satisfy declared effects";

  public static final String EffectAssurance_msgContructorRule = "\"{0}\" checked by constructor rule: writes to a newly created object are invisible";

  public static final String EffectAssurance_msgParameterEvidence = "Parameter \"{0}\" bound to \"{1}\"";
  
  private static final Map<String,String> value2field = new HashMap<String,String>();
  
  public static String getName(String msg) {
	  //Map<String,String> map = value2field;
	  return value2field.get(msg);
  }
  
  private Messages() {
    // private constructor to prevent instantiation
  }

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
    
    collectConstantNames(Messages.class, value2field);
  }
}
