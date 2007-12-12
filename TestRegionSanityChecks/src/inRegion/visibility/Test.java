package inRegion.visibility;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("private PrivateLocal"),
  @Region("DefaultLocal"),
  @Region("protected ProtectedLocal"),
  @Region("public PublicLocal"),
  @Region("private static PrivateStaticLocal"),
  @Region("static DefaultStaticLocal"),
  @Region("protected static ProtectedStaticLocal"),
  @Region("public static PublicStaticLocal"),
})
public class Test extends Super {
  
  // Local instance
  
  @SuppressWarnings("unused")
  @InRegion("PrivateLocal" /* is CONSISTENT */)
  private int private1;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("DefaultLocal" /* is CONSISTENT */)
  private int private2;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("ProtectedLocal" /* is CONSISTENT */)
  private int private3;  // GOOD
  @SuppressWarnings("unused")
  @InRegion("PublicLocal" /* is CONSISTENT */)
  private int private4;     // GOOD

  @InRegion("PrivateLocal" /* is UNASSOCIATED */)
  int default1;   // BAD
  @InRegion("DefaultLocal" /* is CONSISTENT */)
  int default2;   // GOOD
  @InRegion("ProtectedLocal" /* is CONSISTENT */)
  int default3; // GOOD
  @InRegion("PublicLocal" /* is CONSISTENT */)
  int default4;    // GOOD

  @InRegion("PrivateLocal" /* is UNASSOCIATED */)
  protected int protected1;   // BAD
  @InRegion("DefaultLocal" /* is UNASSOCIATED */)
  protected int protected2;   // BAD
  @InRegion("ProtectedLocal" /* is CONSISTENT */)
  protected int protected3; // GOOD
  @InRegion("PublicLocal" /* is CONSISTENT */)
  protected int protected4;    // GOOD

  @InRegion("PrivateLocal" /* is UNASSOCIATED */)
  public int public1;    // BAD
  @InRegion("DefaultLocal" /* is UNASSOCIATED */)
  public int public2;    // BAD
  @InRegion("ProtectedLocal" /* is UNASSOCIATED */)
  public int public3;  // BAD
  @InRegion("PublicLocal" /* is CONSISTENT */)
  public int public4;     // GOOD
  
  // Super instance
  
  @SuppressWarnings("unused")
  @InRegion("PrivateSuper" /* is UNASSOCIATED */)
  private int private10;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("DefaultSuper" /* is CONSISTENT */)
  private int private20;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("ProtectedSuper" /* is CONSISTENT */)
  private int private30;  // GOOD
  @SuppressWarnings("unused")
  @InRegion("PublicSuper" /* is CONSISTENT */)
  private int private40;     // GOOD

  @InRegion("PrivateSuper" /* is UNASSOCIATED */)
  int default10;   // BAD
  @InRegion("DefaultSuper" /* is CONSISTENT */)
  int default20;   // GOOD
  @InRegion("ProtectedSuper" /* is CONSISTENT */)
  int default30; // GOOD
  @InRegion("PublicSuper" /* is CONSISTENT */)
  int default40;    // GOOD

  @InRegion("PrivateSuper" /* is UNASSOCIATED */)
  protected int protected10;   // BAD
  @InRegion("DefaultSuper" /* is UNASSOCIATED */)
  protected int protected20;   // BAD
  @InRegion("ProtectedSuper" /* is CONSISTENT */)
  protected int protected30; // GOOD
  @InRegion("PublicSuper" /* is CONSISTENT */)
  protected int protected40;    // GOOD

  @InRegion("PrivateSuper" /* is UNASSOCIATED */)
  public int public10;    // BAD
  @InRegion("DefaultSuper" /* is UNASSOCIATED */)
  public int public20;    // BAD
  @InRegion("ProtectedSuper" /* is UNASSOCIATED */)
  public int public30;  // BAD
  @InRegion("PublicSuper" /* is CONSISTENT */)
  public int public40;     // GOOD
  
  // Local static
  
  @SuppressWarnings("unused")
  @InRegion("PrivateStaticLocal" /* is CONSISTENT */)
  private static int private100;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("DefaultStaticLocal" /* is CONSISTENT */)
  private static int private200;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("ProtectedStaticLocal" /* is CONSISTENT */)
  private static int private300;  // GOOD
  @SuppressWarnings("unused")
  @InRegion("PublicStaticLocal" /* is CONSISTENT */)
  private static int private400;     // GOOD

  @InRegion("PrivateStaticLocal" /* is UNASSOCIATED */)
  static int default100;   // BAD
  @InRegion("DefaultStaticLocal" /* is CONSISTENT */)
  static int default200;   // GOOD
  @InRegion("ProtectedStaticLocal" /* is CONSISTENT */)
  static int default300; // GOOD
  @InRegion("PublicStaticLocal" /* is CONSISTENT */)
  static int default400;    // GOOD

  @InRegion("PrivateStaticLocal" /* is UNASSOCIATED */)
  protected static int protected100;   // BAD
  @InRegion("DefaultStaticLocal" /* is UNASSOCIATED */)
  protected static int protected200;   // BAD
  @InRegion("ProtectedStaticLocal" /* is CONSISTENT */)
  protected static int protected300; // GOOD
  @InRegion("PublicStaticLocal" /* is CONSISTENT */)
  protected static int protected400;    // GOOD

  @InRegion("PrivateStaticLocal" /* is UNASSOCIATED */)
  public static int public100;    // BAD
  @InRegion("DefaultStaticLocal" /* is UNASSOCIATED */)
  public static int public200;    // BAD
  @InRegion("ProtectedStaticLocal" /* is UNASSOCIATED */)
  public static int public300;  // BAD
  @InRegion("PublicStaticLocal" /* is CONSISTENT */)
  public static int public400;     // GOOD
  
  // Super static
  
  @SuppressWarnings("unused")
  @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
  private static int private1000;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("DefaultStaticSuper" /* is CONSISTENT */)
  private static int private2000;    // GOOD
  @SuppressWarnings("unused")
  @InRegion("ProtectedStaticSuper" /* is CONSISTENT */)
  private static int private3000;  // GOOD
  @SuppressWarnings("unused")
  @InRegion("PublicStaticSuper" /* is CONSISTENT */)
  private static int private4000;     // GOOD

  @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
  static int default1000;   // BAD
  @InRegion("DefaultStaticSuper" /* is CONSISTENT */)
  static int default2000;   // GOOD
  @InRegion("ProtectedStaticSuper" /* is CONSISTENT */)
  static int default3000; // GOOD
  @InRegion("PublicStaticSuper" /* is CONSISTENT */)
  static int default4000;    // GOOD

  @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
  protected static int protected1000;   // BAD
  @InRegion("DefaultStaticSuper" /* is UNASSOCIATED */)
  protected static int protected2000;   // BAD
  @InRegion("ProtectedStaticSuper" /* is CONSISTENT */)
  protected static int protected3000; // GOOD
  @InRegion("PublicStaticSuper" /* is CONSISTENT */)
  protected static int protected4000;    // GOOD

  @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
  public static int public1000;    // BAD
  @InRegion("DefaultStaticSuper" /* is UNASSOCIATED */)
  public static int public2000;    // BAD
  @InRegion("ProtectedStaticSuper" /* is UNASSOCIATED */)
  public static int public3000;  // BAD
  @InRegion("PublicStaticSuper" /* is CONSISTENT */)
  public static int public4000;     // GOOD
}
