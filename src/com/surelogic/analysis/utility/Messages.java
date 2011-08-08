package com.surelogic.analysis.utility;

import java.util.*;

import edu.cmu.cs.fluid.util.AbstractMessages;

public final class Messages extends AbstractMessages {
  // Prevent instantiation
  private Messages() {
    super();
  }
  
  
  
  // Drop-sea result messages
  public static final int CLASS_IS_PUBLIC = 600;
  public static final int CLASS_IS_NOT_PUBLIC = 601;
  public static final int FIELD_IS_STATIC = 606;
  public static final int FIELD_IS_NOT_STATIC = 607;
  public static final int METHOD_IS_STATIC = 608;
  public static final int METHOD_IS_NOT_STATIC = 609;
  public static final int NO_CONSTRUCTOR = 610;
  public static final int TOO_MANY_CONSTRUCTORS = 611;
  public static final int CONSTRUCTOR_NOT_PRIVATE = 612;
  public static final int CONSTRUCTOR_BAD_ARGS = 613;
  public static final int PRIVATE_NO_ARG_CONSTRUCTOR = 614;
  public static final int CONSTRUCTOR_DOES_TOO_MUCH = 615;
  public static final int CONSTRUCTOR_OKAY = 616;
  public static final int CONSTRUCTOR_THROWS_ASSERTION_ERROR = 617;
  public static final int INSTANCE_CREATED = 618;
  public static final int SUBCLASSED = 619;
  public static final int CONSIDER_FINAL = 620;
  public static final int CONSTRUCTOR_COMPILED = 621;
  
  
  
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
