package test;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class Test {
  private void m(final @Unique Object o) {}
  
  @Borrowed("this")
  public Object uniqueReturn1b() {
    // BAD: Borrowed return
    // Trigger borrowed error in opUndefine() via transferMethodBody()
    return this;
  }
  
  @Borrowed("this")
  public void bad1() {
    m(this);
  }
  
  @Unique("return")
  public Test() {
    m(this);
  }
  
  @Borrowed("this")
  public Test(boolean flag) {
    m(this);
  }
}
