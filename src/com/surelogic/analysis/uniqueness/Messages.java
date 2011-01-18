package com.surelogic.analysis.uniqueness;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.sea.Category;

public final class Messages  {
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_UNIQUE_PARAMS_SATISFIED = Category.getInstance2(300);
  public static final Category DSC_UNIQUE_PARAMS_UNSATISFIED = Category.getInstance2(301);
  
  
  
  public static final int METHOD_CONTROL_FLOW = 300;
  public static final int UNIQUE_RETURN = 301;
  public static final int BORROWED_PARAMETERS = 302;
  public static final int UNIQUE_PARAMETERS = 303; // unused?
  public static final int CALL_EFFECT = 304;
  public static final int DEPENDENCY_DROP = 305; // unused?
  public static final int AGGREGATED_UNIQUE_FIELDS = 306;
  public static final int AGGREGATED_UNIQUE_PARAMS = 307;
  public static final int UNIQUE_PARAMETERS_SATISFIED = 308;
  public static final int UNIQUE_PARAMETERS_UNSATISFIED = 309;
  public static final int UNIQUE_RETURN_VALUE = 310;
  public static final int BORROWED_CONSTRUCTOR = 311;
  
  
  
  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
  static {
    code2name.put(METHOD_CONTROL_FLOW, "UniquenessAssurance");
    code2name.put(UNIQUE_RETURN, "UniquenessAssurance_uniqueReturnDrop");
    code2name.put(BORROWED_PARAMETERS, "UniquenessAssurance_borrowedParametersDrop");
    code2name.put(UNIQUE_PARAMETERS, "?");
    code2name.put(CALL_EFFECT, "UniquenessAssurance_effectOfCallDrop");
    code2name.put(DEPENDENCY_DROP, "?");
    code2name.put(AGGREGATED_UNIQUE_FIELDS, "UniquenessAssurance_aggregatedUniqueFields");
    code2name.put(AGGREGATED_UNIQUE_PARAMS, "UniquenessAssurance_aggregatedUniqueParams");
    code2name.put(UNIQUE_PARAMETERS_SATISFIED, "UniquenessAssurance_uniqueParametersDrop");
    code2name.put(UNIQUE_PARAMETERS_UNSATISFIED, "UniquenessAssurance_uniqueParametersDrop");
    code2name.put(UNIQUE_RETURN_VALUE, "?");
    code2name.put(BORROWED_CONSTRUCTOR, "?");
  }
  
  public static String toString(int code) {
    return code2name.get(code);
  }
}
