package test;

import com.surelogic.Aggregate;
import com.surelogic.AggregateInRegion;
import com.surelogic.Assume;
import com.surelogic.Borrowed;
import com.surelogic.Containable;
import com.surelogic.Unique;

@Containable
@SuppressWarnings("unused")
public class TestAssure {
  // Good: primitive
  private int f1 = 0;
  
  // Good: Uniquely referenced, aggregated, containable object
  @Unique
  @Aggregate
  private ContainableClass f2 = null;

  // Good: Uniquely referenced, aggregated, containable object
  @Unique
  @AggregateInRegion("Instance")
  private ContainableClass f3 = null;

  // Bad: Just containable
  @Assume("Containable")
  private ContainableClass f4 = null;
  
  // Bad: Just unique and containable
  @Assume("Containable")
  @Unique
  private ContainableClass f5 = null;
  
  // Bad: Uniquely referenced, aggregated, NON-containable object
  @Assume("Containable")
  @Unique
  @Aggregate
  private NonContainableClass f6 = null;
  
  // Bad: Uniquely referenced, aggregated, NON-containable object
  @Assume("Containable")
  @Unique
  @AggregateInRegion("Instance")
  private NonContainableClass f7 = null;
  
  // Bad: Just Unique
  @Assume("Containable")
  @Unique
  private NonContainableClass f8 = null;
  
  // Bad: Array is not containable
  @Assume("Containable")
  @Unique
  @Aggregate
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