package test;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class Test {
  public static void m1(@Borrowed Object b) {
    m2(b);
  }

  public static void m2(@Borrowed Object b) {
    m3(b);
  }

  public static void m3(@Borrowed Object b) {
    m4(b);
  }

  public static void m4(@Borrowed Object b) {
    m5(b, b);
  }

  public static void m5(@Borrowed Object b, @Borrowed Object c) {
    // do nothing
  }
  
  
  
  @Borrowed("this")
  public void goodBorrowedReceiver() {
    // does nothing
  }

  @Borrowed("this")
  public Test() {
    super();
  }
  
  @Unique("return")
  public Test(boolean f) {
    super();
  }
}

