package test.simple.accessibility.differentPackages.p2;

import test.simple.accessibility.differentPackages.p1.Super;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("private PrivateLocal"),
  @Region("DefaultLocal"),
  @Region("protected ProtectedLocal"),
  @Region("public PublicLocal"),
  @Region("private static PrivateStaticLocal"),
  @Region("static DefaultStaticLocal"),
  @Region("protected static ProtectedStaticLocal"),
  @Region("public static PublicStaticLocal")
})
@SuppressWarnings("unused")
public class ParentIsSuper extends Super {
  // ------ Local Instance Destination Region ------
  
  @UniqueInRegion("PrivateLocal" /* is CONSISTENT */)
  private final Object public1 = new Object();

  @UniqueInRegion("DefaultLocal" /* is CONSISTENT */)
  private final Object public2 = new Object();

  @UniqueInRegion("ProtectedLocal" /* is CONSISTENT */)
  private final Object public3 = new Object();

  @UniqueInRegion("PublicLocal" /* is CONSISTENT */)
  private final Object public4 = new Object();
  
  // ------ Local Static Destination Region ------
  
  @UniqueInRegion("PrivateStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic1 = new Object();

  @UniqueInRegion("DefaultStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic2 = new Object();

  @UniqueInRegion("ProtectedStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic3 = new Object();

  @UniqueInRegion("PublicStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic4 = new Object();
  
  // ------ Local Instance Destination Region ------
  
  @UniqueInRegion("PrivateSuper" /* is UNASSOCIATED */)
  private final Object publicSuper1 = new Object();

  @UniqueInRegion("DefaultSuper" /* is UNASSOCIATED */)
  private final Object publicSuper2 = new Object();

  @UniqueInRegion("ProtectedSuper" /* is CONSISTENT */)
  private final Object publicSuper3 = new Object();

  @UniqueInRegion("PublicSuper" /* is CONSISTENT */)
  private final Object publicSuper4 = new Object();
  
  // ------ Super Static Destination Region ------
  
  @UniqueInRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
  private final Object publicSuperStatic1 = new Object();

  @UniqueInRegion("DefaultStaticSuper" /* is UNASSOCIATED */)
  private final Object publicSuperStatic2 = new Object();

  @UniqueInRegion("ProtectedStaticSuper" /* is CONSISTENT */)
  private final Object publicSuperStatic3 = new Object();

  @UniqueInRegion("PublicStaticSuper" /* is CONSISTENT */)
  private final Object publicSuperStatic4 = new Object();
}
