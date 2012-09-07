package com.surelogic.analysis.threads;

final class Messages {
  private Messages() {
    // private constructor to prevent instantiation
  }

  public static final int NO_THREADS_STARTED = 1;
  public static final int PROHIBITED = 2;
  public static final int CALLED_METHOD_DOES_PROMISE = 3;
  public static final int CALLED_METHOD_DOES_NOT_PROMISE = 4;

}
