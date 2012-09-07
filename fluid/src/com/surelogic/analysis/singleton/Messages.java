package com.surelogic.analysis.singleton;

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

  public static final int CLASS_IS_FINAL = 653;
  public static final int CLASS_NOT_FINAL = 654;
  public static final int CONSTRUCTOR_IS_PRIVATE = 655;
  public static final int CONSTRUCTOR_NOT_PRIVATE = 656;
  public static final int GOOD_CREATION = 657;
  public static final int EXTRA_CREATION = 658;
  public static final int CLASS_ONE_PUBLIC_FIELD = 659;
  public static final int CLASS_ONE_PRIVATE_FIELD = 660;
  public static final int CLASS_NO_PUBLIC_FIELD = 661;
  public static final int CLASS_NO_PRIVATE_FIELD = 662;
  public static final int CLASS_TOO_MANY = 663;
  public static final int CLASS_METHOD_COMPILED = 664;
  public static final int CLASS_FOUND_GETTER = 665;
  public static final int CLASS_NO_GETTER = 666;
  public static final int FIELD_IS_TRANSIENT = 667;
  public static final int FIELD_NOT_TRANSIENT = 668;
  public static final int BAD_READ_RESOLVE_BODY = 669;
  public static final int READ_RESOLVE_BAD = 670;
  public static final int READ_RESOLVE_GOOD = 671;
  public static final int NO_READ_RESOLVE = 672;
}
