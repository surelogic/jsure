package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Test {
  public @Unique Object f1;
  
  public Object other;
  
  public void doesntUse1() {
    other = null;
  }
  
  public void writesIt() {
    f1 = new Object();
  }
  
  @RegionEffects("none")
  public void borrows(@Borrowed Object o) {
    // do nothing
  }
  
  public void readsIt() {
    borrows(f1);    
  }
}