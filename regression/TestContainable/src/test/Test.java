package test;

import com.surelogic.Borrowed;
import com.surelogic.Containable;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@Containable
@SuppressWarnings("unused")
public class Test {
  // Good: primitive
  private int f1 = 0;
  
  // Good: Uniquely referenced, aggregated, containable object
  @Unique
  private ContainableClass f2 = null;

  // Good: Uniquely referenced, aggregated, containable object
  @UniqueInRegion("Instance")
  private ContainableClass f3 = null;

  // Bad: Just containable
  private ContainableClass f4 = null;
  
  // Bad: Uniquely referenced, aggregated, NON-containable object
  @Unique
  private NonContainableClass f6 = null;
  
  // Bad: Uniquely referenced, aggregated, NON-containable object
  @UniqueInRegion("Instance")
  private NonContainableClass f7 = null;
  
  // Bad: Just Unique
  @Unique
  private NonContainableClass f8 = null;
  
  // GOOD: int[] is containable (as of 2011-08-23)
  @Unique
  private int[] f9 = { 0, 1, 2 };
  
  
  // bad
  public Test(int x) {}
  
  // Good
  @Unique("return")
  public Test(int x, int y) {}
  
  // Good
  @Borrowed("this")
  public Test(int x, int y, int z) {}
  
  
  
  // Bad
  public void bad() {}
  
  // Good
  @Borrowed("this")
  public void good() {}
  
  // GOod
  public static void staticMethod() {}
}