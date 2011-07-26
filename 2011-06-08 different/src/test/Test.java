package test;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

// Test getting results on the normal and abrupt paths -- no way to have error only on the normal path?
public class Test {
  private @Unique Object u;
  private @Unique Object v;
  private @Unique Object w;
  
  
  
  @RegionEffects("none")
  private static void compromiseRef(final Object o) {
    // do nothing
  }

  // bad on both edges
  private void bad1() {
	compromiseRef(u);
  }
  
  // only bad on the abrupt edge
  private void bad2() {
	compromiseRef(u);
    u = new Object();
  }
}


