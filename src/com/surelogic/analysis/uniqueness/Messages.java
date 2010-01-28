package com.surelogic.analysis.uniqueness;

public class Messages  {
  
  public static final String Category_uniqueParametersSatisfied = "Unique precondition(s) satisfied";
  
  public static final String Category_uniqueParametersUnsatisfied = "Unique precondition(s) unsatisfied";
  
  
  
  public static final int methodControlFlow = 300;

  public static final int uniqueReturnDrop = 301;

  public static final int borrowedParametersDrop = 302;

  public static final int uniqueParametersDrop = 303;

  public static final int effectOfCallDrop = 304;

  public static final int dependencyDrop = 305;

  public static final int aggregatedUniqueFields = 306;

  public static final int aggregatedUniqueParams = 307;
  
  public static final int uniqueParametersSatisfied = 308;
  
  public static final int uniqueParametersUnsatisfied = 309;
  
  
  public static final int uniqueReturnValue = 310;
  
  public static final int borrowedConstructor = 311;
  
  private Messages() {
    // private constructor to prevent instantiation
  }
}
