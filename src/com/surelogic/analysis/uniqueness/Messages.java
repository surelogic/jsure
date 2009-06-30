package com.surelogic.analysis.uniqueness;

//import com.surelogic.javasure.analysis.messages.Messages;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  
  private static final String BUNDLE_NAME = "com.surelogic.analysis.unique.messages"; //$NON-NLS-1$
 
  public static final String UniquenessAssurance_uniquenessContraints1 = "Method body respects uniqueness constraints";

  public static final String UniquenessAssurance_uniquenessContraints2 = "Method body does not respect uniqueness constraints";

//  public static final String UniquenessAssurance_checkMethodCallDrop = "{0} [-cMC: isInvalid ==  {1} , msg = {2} ]";

  public static final String UniquenessAssurance_uniqueReturnDrop = "Unique return value of call \"";

  public static final String UniquenessAssurance_borrowedParametersDrop = "Borrowed parameters of call \"";

  public static final String UniquenessAssurance_uniqueParametersDrop = "Unique parameters of call \"";

  public static final String UniquenessAssurance_effectOfCallDrop = "Effects of call \"";

  public static final String UniquenessAssurance_dependencyDrop = "Conservative dependency";

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
