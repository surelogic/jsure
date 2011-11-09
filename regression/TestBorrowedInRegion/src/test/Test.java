package test;

import com.surelogic.Borrowed;

@SuppressWarnings("unused")
public class Test {
  // BAD: Field is primitive
  @Borrowed
  private final int bad1 = 0;
  
  // GOOD: Field is reference-typed
  @Borrowed
  private final Object good1 = new Object();
  
  // BAD: Must be final
  @Borrowed
  private volatile Object bad2 = new Object();
  
  
  
  // Bad: must be final
  @Borrowed
  private Object bad10 = new Object();
  
  // Bad: non-final and static
  @Borrowed
  private static Object bad11 = new Object();
  
  // bad: static
  @Borrowed
  private final static Object bad12 = new Object();
}
