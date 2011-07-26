package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;

public class Test2 {
  public Object other;
  
  public void doesntUse1() {
    other = null;
  }
  
  public void writesIt(final Test t) {
    t.f1 = new Object();
  }
  
  @RegionEffects("none")
  public void borrows(@Borrowed Object o) {
    // do nothing
  }
  
  public void readsIt(final Test t) {
    borrows(t.f1);    
  }
}