package test;

import com.surelogic.Utility;

@Utility
@SuppressWarnings("unused")
public final class TestStatic {
  private TestStatic() {
    super();
  }
  
  
  
  private static int good1;
  private static int good2, good3;
  private static int good4, good5, good6;
  
  private int bad1;
  private int bad2, bad3;
  private int bad4, bad5, bad6;

  
  
  public static int goodMethod1(int x) { return 0; }
  public static void goodMethod2() {}
  
  private void badMethod1() {}
  private Object badMethod2(Object o) { return null; }
}
