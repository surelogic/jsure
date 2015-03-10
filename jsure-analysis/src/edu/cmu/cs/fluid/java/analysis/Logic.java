package edu.cmu.cs.fluid.java.analysis;

public class Logic {
  public static final int INCOMPARABLE = 0;
  public static final int ANDABLE = 1;
  public static final int ORABLE = 2;
  public static final int ANDORABLE = ANDABLE | ORABLE;
  public static final int IMPLIES = ANDORABLE | 4;
  public static final int IMPLIED = ANDORABLE | 8;
  public static final int EQUAL = IMPLIES | IMPLIED;
  public static final int CONFLICT = ANDABLE | 16;
  public static final int COMPLEMENT = ORABLE | 32;
  public static final int INVERSE = CONFLICT | COMPLEMENT;

  public static boolean atLeast(int v1, int v2) {
    return ((v1 & v2) == v2);
  }
}
