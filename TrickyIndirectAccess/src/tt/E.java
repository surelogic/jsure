package tt;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;

public class E {
  public int f1;
  public int f2;
  public int f3;
  
  public static E getE() {
    return new E();
  }
  
  @Borrowed("this")
  public E() {
    super();
  }
  
  @Borrowed("this")
  @RegionEffects("writes this:Instance")
  public void doStuff() {
    // Who cares
  }
}
