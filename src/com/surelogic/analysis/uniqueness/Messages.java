package com.surelogic.analysis.uniqueness;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  
  private static final String BUNDLE_NAME = "com.surelogic.analysis.unique.messages"; //$NON-NLS-1$
  
  
  
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
  
  
  public static final String uniqueReturnValue = "May depend on unique return value of {0}";
  
  public static final String borrowedConstructor = "May depend on unique object contructed by {0}";
  
  
  private static final Map<String,String> value2field = new HashMap<String,String>();
  
  public static String getName(String msg) {
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
