package com.surelogic.analysis.uniqueness;

public class Messages  {
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final String CATEGORY_UNIQUE_PARAMETERS_SATISFIED = "Unique precondition(s) satisfied";
  
  public static final String CATEGORY_UNIQUE_PARAMETERS_UNSATISFIED = "Unique precondition(s) unsatisfied";
  
  
  
  public static final int METHOD_CONTROL_FLOW = 300;

  public static final int UNIQUE_RETURN = 301;

  public static final int BORROWED_PARAMETERS = 302;

  public static final int UNIQUE_PARAMETERS = 303;

  public static final int CALL_EFFECT = 304;

  public static final int DEPENDENCY_DROP = 305;

  public static final int AGGREGATED_UNIQUE_FIELDS = 306;

  public static final int AGGREGATED_UNIQUE_PARAMS = 307;
  
  public static final int UNIQUE_PARAMETERS_SATISFIED = 308;
  
  public static final int UNIQUE_PARAMETERS_UNSATISFIED = 309;
  
  
  
  public static final int UNIQUE_RETURN_VALUE = 310;
  
  public static final int BORROWED_CONSTRUCTOR = 311;
}
