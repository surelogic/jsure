package test;

import com.surelogic.Vouch;
import com.surelogic.Borrowed;
import com.surelogic.Containable;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@Containable
@SuppressWarnings("unused")
public class TestAssure {
  // Good: primitive
  private int f1 = 0;
  
  // Good: Uniquely referenced, aggregated, containable object
  @Unique
  private ContainableClass f2 = null;

  // Good: Uniquely referenced, aggregated, containable object
  @UniqueInRegion("Instance")
  private ContainableClass f3 = null;

  // Bad: Just containable
  @Vouch("Containable")
  private ContainableClass f4 = null;
  
  // Bad: Uniquely referenced, aggregated, NON-containable object
  @Vouch("Containable")
  @Unique
  private NonContainableClass f6 = null;
  
  // Bad: Uniquely referenced, aggregated, NON-containable object
  @Vouch("Containable")
  @UniqueInRegion("Instance")
  private NonContainableClass f7 = null;
  
  // Bad: Just Unique
  @Vouch("Containable")
  @Unique
  private NonContainableClass f8 = null;
  
  // Bad: Array is not containable
  @Vouch("Containable")
  @Unique
  private int[] f9 = { 0, 1, 2 };
  

  
  // Good
  @Unique("return")
  public TestAssure(int x, int y) {}
  
  // Good
  @Borrowed("this")
  public TestAssure(int x, int y, int z) {}
  
  
  
  // Good
  @Borrowed("this")
  public void good() {}
  
  // GOod
  public static void staticMethod() {}
}