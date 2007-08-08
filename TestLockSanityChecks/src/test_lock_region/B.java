package test_lock_region;

import com.surelogic.Region;

@Region("static StaticRegionFromB")
public class B {
  protected static final Object staticFieldFromB = new Object();
}
