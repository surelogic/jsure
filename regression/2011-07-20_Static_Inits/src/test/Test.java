package test;

import com.surelogic.Unique;

public class Test {
  private static @Unique Object f1 = null;
  private static @Unique Object f2 = new Object();

  private static Object notUnique() {
    return null;
  }

  static {
    f1 = new Object();
    f2 = new Object();
  }
  
  static {
    f1 = null;
    f2 = null;
  }
  
  static {
    f1 = notUnique();
    f2 = notUnique();
  }
  
  public static void m() {
    f1 = notUnique();
  }
}

