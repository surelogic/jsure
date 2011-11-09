package test;

import com.surelogic.Borrowed;
import com.surelogic.Containable;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@Containable(verify=false)
@SuppressWarnings("unused")
public class TestNoVerify {
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
  
  // Bad: Array is not containable
  @Unique
  private int[] f9 = { 0, 1, 2 };
  
  
  // bad
  public TestNoVerify(int x) {}
  
  // Good
  @Unique("return")
  public TestNoVerify(int x, int y) {}
  
  // Good
  @Borrowed("this")
  public TestNoVerify(int x, int y, int z) {}
  
  
  
  // Bad
  public void bad() {}
  
  // Good
  @Borrowed("this")
  public void good() {}
  
  // GOod
  public static void staticMethod() {}
}