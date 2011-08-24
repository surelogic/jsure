package com.surelogic.analysis.singleton;

import java.util.*;

import edu.cmu.cs.fluid.util.AbstractMessages;

public final class Messages extends AbstractMessages {
  // Prevent instantiation
  private Messages() {
    super();
  }
  
  
  
  // Drop-sea result messages
  public static final int ENUM_ONE_ELEMENT = 650;
  public static final int ENUM_TOO_MANY_ELEMENTS = 651;
  public static final int ENUM_NO_ELEMENTS = 652;
  
  
  
  private static final Map<Integer,String> code2name = new HashMap<Integer,String>();

  /** To support JSure-Sierra integration
  */
  public static String toString(int code) {
	  return code2name.get(code);
  }

  static {
    collectCodeNames(Messages.class, code2name);
  }
}
