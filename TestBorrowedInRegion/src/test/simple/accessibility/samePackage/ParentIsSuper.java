package test.simple.accessibility.samePackage;

import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.BorrowedInRegion;

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
  @BorrowedInRegion("PrivateLocal" /* is CONSISTENT */)
  private final Object public1 = new Object();

  @BorrowedInRegion("DefaultLocal" /* is CONSISTENT */)
  private final Object public2 = new Object();

  @BorrowedInRegion("ProtectedLocal" /* is CONSISTENT */)
  private final Object public3 = new Object();

  @BorrowedInRegion("PublicLocal" /* is CONSISTENT */)
  private final Object public4 = new Object();
  
  // ------ Local Static Destination Region ------
  
  @BorrowedInRegion("PrivateStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic1 = new Object();

  @BorrowedInRegion("DefaultStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic2 = new Object();

  @BorrowedInRegion("ProtectedStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic3 = new Object();

  @BorrowedInRegion("PublicStaticLocal" /* is CONSISTENT */)
  private final Object publicStatic4 = new Object();
  
  // ------ Super Instance Destination Region ------
  
  @BorrowedInRegion("PrivateSuper" /* is UNASSOCIATED */)
  private final Object publicSuper1 = new Object();

  @BorrowedInRegion("DefaultSuper" /* is CONSISTENT */)
  private final Object publicSuper2 = new Object();

  @BorrowedInRegion("ProtectedSuper" /* is CONSISTENT */)
  private final Object publicSuper3 = new Object();

  @BorrowedInRegion("PublicSuper" /* is CONSISTENT */)
  private final Object publicSuper4 = new Object();
  
  // ------ Super Static Destination Region ------
  
  @BorrowedInRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
  private final Object publicSuperStatic1 = new Object();

  @BorrowedInRegion("DefaultStaticSuper" /* is CONSISTENT */)
  private final Object publicSuperStatic2 = new Object();

  @BorrowedInRegion("ProtectedStaticSuper" /* is CONSISTENT */)
  private final Object publicSuperStatic3 = new Object();

  @BorrowedInRegion("PublicStaticSuper" /* is CONSISTENT */)
  private final Object publicSuperStatic4 = new Object();
}
