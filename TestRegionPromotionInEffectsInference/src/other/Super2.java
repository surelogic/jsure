package other;

import com.surelogic.InRegion;

import test.Super;

public class Super2 extends Super {
  @InRegion("SuperProtected")
  protected int ppp;
  
  
  
  public void publicMethod() {
    ppp = 1;
  }

  protected void protectedMethod() {
    ppp = 1;
  }

  void defaultMethod() {
    ppp = 1;
  }

  private void privateMethod() {
    ppp = 1;
  }
}
