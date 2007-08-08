package test_lock_region;

import com.surelogic.Region;

@Region("static StaticRegionFromD")
public class D extends C {
  protected static final Object staticFieldFromD = new Object();
}
