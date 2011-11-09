package test.norepeats;

import com.surelogic.RegionEffects;
import com.surelogic.Starts;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;


@SuppressWarnings("unused")
public final class C {
  // Good
  @Unique
  private final Object good1 = new Object();

  // Good
  @UniqueInRegion("Instance")
  private final Object good2 = new Object();

  // Good
  @UniqueInRegion("Instance into Instance")
  private final Object good3 = new Object();
  
  // Bad
  @Unique
  @UniqueInRegion("Instance")
  private final Object bad1 = new Object();
  
  // Bad
  @Unique
  @UniqueInRegion("Instance into Instance")
  private final Object bad2 = new Object();

  // Not possible
//  @UniqueInRegion("Instance")
//  @UniqueInRegion("Instance into Instance")
//  private final Object bad3 = new Object();
}
