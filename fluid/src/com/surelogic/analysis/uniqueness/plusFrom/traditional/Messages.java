package com.surelogic.analysis.uniqueness.plusFrom.traditional;

import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.sea.Category;

public final class Messages {
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

  public static final int ASSIGNED_UNDEFINED_BY = 318;
  public static final int BORROWED_PASSED_TO_BORROWED = 319;
  public static final int COMPROMISED_READ = 320;
  public static final int COMPROMISED_INDIRECT_READ = 321;
  public static final int LOST_COMPROMISED_FIELD = 322;
  public static final int COMPROMISED_BY = 323;
  public static final int UNDEFINED_BY = 324;
  public static final int READ_OF_BURIED = 325;
  public static final int BURIED_BY = 326;
  public static final int SHARED_NOT_UNIQUE_ACTUAL = 327;
  public static final int SHARED_NOT_UNIQUE_RETURN = 328;
  public static final int SHARED_NOT_UNIQUE_ASSIGN = 329;
  public static final int BORROWED_NOT_UNIQUE_ACTUAL = 330;
  public static final int BORROWED_NOT_UNIQUE_RETURN = 331;
  public static final int BORROWED_NOT_UNIQUE_ASSIGN = 332;
  public static final int BORROWED_AS_UNIQUE = 333;
  public static final int BORROWED_AS_UNIQUE_RETURN = 334;
  public static final int BORROWED_AS_SHARED = 335;
  public static final int BORROWED_AS_SHARED_RETURN = 336;
  public static final int ACTUAL_IS_UNDEFINED = 337;
  public static final int BY_UNIQUE_PARAMETER = 338;
  public static final int BY_UNIQUE_LOAD = 339;
  public static final int BY_SIDE_EFFECT = 340;
  public static final int INVARIANTS_RESPECTED = 341;
  public static final int BORROWED_SATISFIED = 342;
  public static final int ACTUAL_IS_UNIQUE = 343;
  public static final int RETURN_IS_UNIQUE = 344;
  public static final int ASSIGN_IS_UNIQUE = 345;
  public static final int ACTUAL_IS_NULL = 346;
  public static final int RETURN_IS_NULL = 347;
  public static final int ASSIGN_IS_NULL = 348;
  public static final int CONTROL_FLOW_ROOT = 349;
}
