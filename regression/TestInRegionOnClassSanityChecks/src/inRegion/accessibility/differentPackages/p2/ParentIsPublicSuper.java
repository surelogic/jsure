package inRegion.accessibility.differentPackages.p2;

import inRegion.accessibility.differentPackages.p1.PublicSuper;

import com.surelogic.InRegion;
import com.surelogic.InRegions;
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
  @Region("public static PublicStaticLocal")
})  
@SuppressWarnings("unused")
@InRegions({
  @InRegion("Private1 into PrivateLocal" /* is CONSISTENT */),
  @InRegion("Private2 into DefaultLocal" /* is CONSISTENT */),
  @InRegion("Private3 into ProtectedLocal" /* is CONSISTENT */),
  @InRegion("Private4 into PublicLocal" /* is CONSISTENT */),

  @InRegion("Default1 into PrivateLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Default2 into DefaultLocal" /* is CONSISTENT */),
  @InRegion("Default3 into ProtectedLocal" /* is CONSISTENT */),
  @InRegion("Default4 into PublicLocal" /* is CONSISTENT */),

  @InRegion("Protected1 into PrivateLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Protected2 into DefaultLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Protected3 into ProtectedLocal" /* is CONSISTENT */),
  @InRegion("Protected4 into PublicLocal" /* is CONSISTENT */),

  @InRegion("Public1 into PrivateLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public2 into DefaultLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public3 into ProtectedLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public4 into PublicLocal" /* is CONSISTENT */),

  @InRegion("Private10 into PrivateSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Private20 into DefaultSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Private30 into ProtectedSuper" /* is CONSISTENT */),
  @InRegion("Private40 into PublicSuper" /* is CONSISTENT */),

  @InRegion("Default10 into PrivateSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Default20 into DefaultSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Default30 into ProtectedSuper" /* is CONSISTENT */),
  @InRegion("Default40 into PublicSuper" /* is CONSISTENT */),

  @InRegion("Protected10 into PrivateSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Protected20 into DefaultSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Protected30 into ProtectedSuper" /* is CONSISTENT */),
  @InRegion("Protected40 into PublicSuper" /* is CONSISTENT */),

  @InRegion("Public10 into PrivateSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Public20 into DefaultSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Public30 into ProtectedSuper" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public40 into PublicSuper" /* is CONSISTENT */),

  @InRegion("Private100 into PrivateStaticLocal" /* is CONSISTENT */),
  @InRegion("Private200 into DefaultStaticLocal" /* is CONSISTENT */),
  @InRegion("Private300 into ProtectedStaticLocal" /* is CONSISTENT */),
  @InRegion("Private400 into PublicStaticLocal" /* is CONSISTENT */),

  @InRegion("Default100 into PrivateStaticLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Default200 into DefaultStaticLocal" /* is CONSISTENT */),
  @InRegion("Default300 into ProtectedStaticLocal" /* is CONSISTENT */),
  @InRegion("Default400 into PublicStaticLocal" /* is CONSISTENT */),

  @InRegion("Protected100 into PrivateStaticLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Protected200 into DefaultStaticLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Protected300 into ProtectedStaticLocal" /* is CONSISTENT */),
  @InRegion("Protected400 into PublicStaticLocal" /* is CONSISTENT */),

  @InRegion("Public100 into PrivateStaticLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public200 into DefaultStaticLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public300 into ProtectedStaticLocal" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public400 into PublicStaticLocal" /* is CONSISTENT */),

  @InRegion("Private1000 into PrivateStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Private2000 into DefaultStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Private3000 into ProtectedStaticSuper" /* is CONSISTENT */),
  @InRegion("Private4000 into PublicStaticSuper" /* is CONSISTENT */),

  @InRegion("Default1000 into PrivateStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Default2000 into DefaultStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Default3000 into ProtectedStaticSuper" /* is CONSISTENT */),
  @InRegion("Default4000 into PublicStaticSuper" /* is CONSISTENT */),

  @InRegion("Protected1000 into PrivateStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Protected2000 into DefaultStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Protected3000 into ProtectedStaticSuper" /* is CONSISTENT */),
  @InRegion("Protected4000 into PublicStaticSuper" /* is CONSISTENT */),

  @InRegion("Public1000 into PrivateStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Public2000 into DefaultStaticSuper" /* is UNASSOCIATED: Parent region is not accessible */),
  @InRegion("Public3000 into ProtectedStaticSuper" /* is UNASSOCIATED: More visible than parent */),
  @InRegion("Public4000 into PublicStaticSuper" /* is CONSISTENT */),
})
public class ParentIsPublicSuper extends PublicSuper {
  // Local instance
  
  private int Private1; /* is CONSISTENT */    
  private int Private2; /* is CONSISTENT */    
  private int Private3; /* is CONSISTENT */    
  private int Private4; /* is CONSISTENT */    

  int Default1; /* is UNASSOCIATED: More visible than parent */    
  int Default2; /* is CONSISTENT */    
  int Default3; /* is CONSISTENT */    
  int Default4; /* is CONSISTENT */    

  protected int Protected1; /* is UNASSOCIATED: More visible than parent */    
  protected int Protected2; /* is UNASSOCIATED: More visible than parent */    
  protected int Protected3; /* is CONSISTENT */    
  protected int Protected4; /* is CONSISTENT */    

  public int Public1; /* is UNASSOCIATED: More visible than parent */    
  public int Public2; /* is UNASSOCIATED: More visible than parent */    
  public int Public3; /* is UNASSOCIATED: More visible than parent */    
  public int Public4; /* is CONSISTENT */    
  
  // Super instance
  
  private int Private10; /* is UNASSOCIATED: Parent is not accessible */    
  private int Private20; /* is UNASSOCIATED: Parent is not accessible */    
  private int Private30; /* is CONSISTENT */    
  private int Private40; /* is CONSISTENT */    

  int Default10; /* is UNASSOCIATED: Parent is not accessible */    
  int Default20; /* is UNASSOCIATED: Parent is not accessible */    
  int Default30; /* is CONSISTENT */    
  int Default40; /* is CONSISTENT */    

  protected int Protected10; /* is UNASSOCIATED: Parent is not accessible */    
  protected int Protected20; /* is UNASSOCIATED: Parent is not accessible */    
  protected int Protected30; /* is CONSISTENT */    
  protected int Protected40; /* is CONSISTENT */    

  public int Public10; /* is UNASSOCIATED: Parent is not accessible */    
  public int Public20; /* is UNASSOCIATED: Parent is not accessible */    
  public int Public30; /* is UNASSOCIATED: More visible than parent */    
  public int Public40; /* is CONSISTENT */    
  
  // Local static
  
  private static int Private100; /* is CONSISTENT */    
  private static int Private200; /* is CONSISTENT */    
  private static int Private300; /* is CONSISTENT */    
  private static int Private400; /* is CONSISTENT */    

  static int Default100; /* is UNASSOCIATED: More visible than parent */    
  static int Default200; /* is CONSISTENT */    
  static int Default300; /* is CONSISTENT */    
  static int Default400; /* is CONSISTENT */    

  protected static int Protected100; /* is UNASSOCIATED: More visible than parent */    
  protected static int Protected200; /* is UNASSOCIATED: More visible than parent */    
  protected static int Protected300; /* is CONSISTENT */    
  protected static int Protected400; /* is CONSISTENT */    

  public static int Public100; /* is UNASSOCIATED: More visible than parent */    
  public static int Public200; /* is UNASSOCIATED: More visible than parent */    
  public static int Public300; /* is UNASSOCIATED: More visible than parent */    
  public static int Public400; /* is CONSISTENT */    
  
  // Super static
  
  private static int Private1000; /* is UNASSOCIATED: Parent is not accessible */    
  private static int Private2000; /* is UNASSOCIATED: Parent is not accessible */    
  private static int Private3000; /* is CONSISTENT */    
  private static int Private4000; /* is CONSISTENT */    

  static int Default1000; /* is UNASSOCIATED: Parent is not accessible */    
  static int Default2000; /* is UNASSOCIATED: Parent is not accessible */    
  static int Default3000; /* is CONSISTENT */    
  static int Default4000; /* is CONSISTENT */    

  protected static int Protected1000; /* is UNASSOCIATED: Parent is not accessible */    
  protected static int Protected2000; /* is UNASSOCIATED: Parent is not accessible */    
  protected static int Protected3000; /* is CONSISTENT */    
  protected static int Protected4000; /* is CONSISTENT */    

  public static int Public1000; /* is UNASSOCIATED: Parent is not accessible */    
  public static int Public2000; /* is UNASSOCIATED: Parent is not accessible */    
  public static int Public3000; /* is UNASSOCIATED: More visible than parent */    
  public static int Public4000; /* is CONSISTENT */    
}
