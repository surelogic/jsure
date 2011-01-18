package com.surelogic.analysis.testing;

import edu.cmu.cs.fluid.sea.Category;

final class Messages  {
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_BCA = Category.getInstance2(500);
  public static final Category DSC_COLLECT_METHOD_CALLS = Category.getInstance2(510);
  public static final Category DSC_LOCAL_VARIABLES = Category.getInstance2(520);
  
  
  public static final int BINDS_TO = 500;
  public static final int CALLS = 510;
  public static final int LOCAL_VARS = 520;
  
  
  
  /* Don't need this yet. */
  
//  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
//  static {
//    code2name.put(METHOD_CONTROL_FLOW, "UniquenessAssurance");
//    code2name.put(UNIQUE_RETURN, "UniquenessAssurance_uniqueReturnDrop");
//    code2name.put(BORROWED_PARAMETERS, "UniquenessAssurance_borrowedParametersDrop");
//    code2name.put(UNIQUE_PARAMETERS, "?");
//    code2name.put(CALL_EFFECT, "UniquenessAssurance_effectOfCallDrop");
//    code2name.put(DEPENDENCY_DROP, "?");
//    code2name.put(AGGREGATED_UNIQUE_FIELDS, "UniquenessAssurance_aggregatedUniqueFields");
//    code2name.put(AGGREGATED_UNIQUE_PARAMS, "UniquenessAssurance_aggregatedUniqueParams");
//    code2name.put(UNIQUE_PARAMETERS_SATISFIED, "UniquenessAssurance_uniqueParametersDrop");
//    code2name.put(UNIQUE_PARAMETERS_UNSATISFIED, "UniquenessAssurance_uniqueParametersDrop");
//    code2name.put(UNIQUE_RETURN_VALUE, "?");
//    code2name.put(BORROWED_CONSTRUCTOR, "?");
//  }
//  
//  public static String toString(int code) {
//    return code2name.get(code);
//  }
}
