package com.surelogic.analysis.uniqueness;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  
  private static final String BUNDLE_NAME = "com.surelogic.analysis.unique.messages"; //$NON-NLS-1$
  
  
  
  public static final String Category_uniqueParametersSatisfied = "Unique precondition(s) satisfied";
  
  public static final String Category_uniqueParametersUnsatisfied = "Unique precondition(s) unsatisfied";
  
  
  
  public static final String methodControlFlow = "Control flow of {0} {1}";

  public static final String uniqueReturnDrop = "Unique return value of call {0}";

  public static final String borrowedParametersDrop = "Borrowed parameters of call {0}";

  public static final String uniqueParametersDrop = "Unique parameters of call {0}";

  public static final String effectOfCallDrop = "Effects of call {0}";

  public static final String dependencyDrop = "Assurance conservatively depends on other annotations";

  public static final String aggregatedUniqueFields = "Assurance conservatively depends on the unique fields accessed in method {0}";

  public static final String aggregatedUniqueParams = "Assurance conservatively depends on the unique parameters of method {0}";

  
  
  public static final String uniqueParametersSatisfied = "Uniqueness preconditions satisfied when calling {0}";
  
  public static final String uniqueParametersUnsatisfied = "Uniqueness preconditions not satisfied when calling {0}";
  
  
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
