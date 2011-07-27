package test;

import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Test {
  /** Unique reference to an Other object */
  private @Unique Object u = shared();
  
//  {
//    u = shared();
//  }
  
  @RegionEffects("none")
  public Object shared() { return null; }
  
  public Test() {
    super();
//    u = shared();
  }
  
  public Test(int other) {
    super();
  }
  
  public void good() {
    u = null;
  }
}

