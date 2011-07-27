package test;

import com.surelogic.Unique;

public class Test {
  public void m(final @Unique Object p1) {
    // do nothing
  }
  
  public void call1(final @Unique Object o) {
    m(o);
  }
  
  public void call2(final Object o) {
    m(o);
  }
  
  public void call3() {
    m(new Object());
  }
}

