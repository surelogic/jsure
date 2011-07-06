package com.surelogic.analysis.uniqueness;

import java.util.HashMap;
import java.util.Map;

import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.sea.Category;

public final class Messages  {
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_UNIQUE_PARAMS_SATISFIED = Category.getInstance(300);
  public static final Category DSC_UNIQUE_PARAMS_UNSATISFIED = Category.getInstance(301);
  public static final Category DSC_UNIQUENESS_TIMEOUT = Category.getInstance(302);
  public static final Category DSC_UNIQUENESS_LONG_RUNNING = Category.getInstance(303);
  
  public static final String NORMAL_EXIT = I18N.misc(320);
  public static final String ABRUPT_EXIT = I18N.misc(321);
  
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
  public static final int TIMEOUT = 312;
  public static final int TOO_LONG = 313;

  public static final int COMPROMISED_READ = 320;
  public static final int COMPROMISED_INDIRECT_READ = 321;  
  public static final int LOST_COMPROMISED_FIELD = 322;
  public static final int COMPROMISED_BY = 323;
  public static final int UNDEFINED_BY = 324;  
  public static final int READ_OF_BURIED = 325;
  public static final int BURIED_BY = 326;
  public static final int SHARED_NOT_UNIQUE = 327;
  public static final int SHARED_NOT_UNIQUE_RETURN = 328;
  public static final int BORROWED_NOT_UNIQUE = 329;
  public static final int BORROWED_NOT_UNIQUE_RETURN = 330;
  public static final int UNDEFINED_NOT_UNIQUE = 331;
  public static final int UNDEFINED_NOT_UNIQUE_RETURN = 332;
  public static final int BORROWED_AS_UNIQUE = 333;
  public static final int BORROWED_AS_UNIQUE_RETURN = 334;
  public static final int BORROWED_AS_SHARED = 335;
  public static final int BORROWED_AS_SHARED_RETURN = 336;
  public static final int UNDEFINED_NOT_BORROWED = 337;
  
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
    code2name.put(TIMEOUT, "?");
    code2name.put(COMPROMISED_READ, "?");
    code2name.put(COMPROMISED_INDIRECT_READ, "?");
    code2name.put(LOST_COMPROMISED_FIELD, "?");
    code2name.put(COMPROMISED_BY, "?");
    code2name.put(UNDEFINED_BY, "?");
    code2name.put(READ_OF_BURIED, "?");
    code2name.put(BURIED_BY, "?");
    code2name.put(SHARED_NOT_UNIQUE, "?");
    code2name.put(SHARED_NOT_UNIQUE_RETURN, "?");
    code2name.put(BORROWED_NOT_UNIQUE, "?");
    code2name.put(BORROWED_NOT_UNIQUE_RETURN, "?");
    code2name.put(BORROWED_AS_UNIQUE, "?");
    code2name.put(BORROWED_AS_UNIQUE_RETURN, "?");
    code2name.put(UNDEFINED_NOT_UNIQUE, "?");
    code2name.put(UNDEFINED_NOT_UNIQUE_RETURN, "?");
    code2name.put(BORROWED_AS_SHARED, "?");
    code2name.put(BORROWED_AS_SHARED_RETURN, "?");
    code2name.put(UNDEFINED_NOT_BORROWED, "?");
  }
  
  public static String toString(int code) {
    return code2name.get(code);
  }
}
