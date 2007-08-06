package test_lock_region;

import com.surelogic.Region;

@Region("static StaticRegionFromOther")
public class Other {
  protected static final Object staticFieldFromOther = new Object();
}
