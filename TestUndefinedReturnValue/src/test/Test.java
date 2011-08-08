package test;

import com.surelogic.Unique;

/*
 * Originally test for bug 1711
 */
public class Test {
  @Unique
  private final M cache = null;
   
  public Test standard() {
    return null;
  }
  
  
  public Test forFields() {
    cache.isEmpty();
    Test t = standard();
    return t;
  }
  
  public void makeCall() {
    cache.isEmpty();
    callMe(standard());
  }
  
  private void callMe(final Object o) {} 
}

class M {
  @Unique("return")
  public M() {
    super();
  }
  
  // MISSING EFFECTS!
  public boolean isEmpty() { return false; }
}
