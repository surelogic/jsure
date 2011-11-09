package inRegion.accessibility.samePackage;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("private PrivateSuper"),
  @Region("DefaultSuper"),
  @Region("protected ProtectedSuper"),
  @Region("public PublicSuper"),
  @Region("private static PrivateStaticSuper"),
  @Region("static DefaultStaticSuper"),
  @Region("protected static ProtectedStaticSuper"),
  @Region("public static PublicStaticSuper")
})
public class PublicSuper {
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
  public static class Inner_ParentIsPublicSuper extends PublicSuper {
    // Local instance
    
    @SuppressWarnings("unused")
    @InRegion("PrivateLocal" /* is CONSISTENT */)
    private int Private1;    
    @SuppressWarnings("unused")
    @InRegion("DefaultLocal" /* is CONSISTENT */)
    private int Private2;    
    @SuppressWarnings("unused")
    @InRegion("ProtectedLocal" /* is CONSISTENT */)
    private int Private3;  
    @SuppressWarnings("unused")
    @InRegion("PublicLocal" /* is CONSISTENT */)
    private int Private4;     

    @InRegion("PrivateLocal" /* is UNASSOCIATED */)
    int Default1;   
    @InRegion("DefaultLocal" /* is CONSISTENT */)
    int Default2;   
    @InRegion("ProtectedLocal" /* is CONSISTENT */)
    int Default3; 
    @InRegion("PublicLocal" /* is CONSISTENT */)
    int Default4;    

    @InRegion("PrivateLocal" /* is UNASSOCIATED */)
    protected int Protected1;   
    @InRegion("DefaultLocal" /* is UNASSOCIATED */)
    protected int Protected2;   
    @InRegion("ProtectedLocal" /* is CONSISTENT */)
    protected int Protected3; 
    @InRegion("PublicLocal" /* is CONSISTENT */)
    protected int Protected4;    

    @InRegion("PrivateLocal" /* is UNASSOCIATED */)
    public int Public1;    
    @InRegion("DefaultLocal" /* is UNASSOCIATED */)
    public int Public2;    
    @InRegion("ProtectedLocal" /* is UNASSOCIATED */)
    public int Public3;  
    @InRegion("PublicLocal" /* is CONSISTENT */)
    public int Public4;     
    
    // Super instance
    
    @SuppressWarnings("unused")
    @InRegion("PrivateSuper" /* is CONSISTENT */)
    private int Private10;    
    @SuppressWarnings("unused")
    @InRegion("DefaultSuper" /* is CONSISTENT */)
    private int Private20;    
    @SuppressWarnings("unused")
    @InRegion("ProtectedSuper" /* is CONSISTENT */)
    private int Private30;  
    @SuppressWarnings("unused")
    @InRegion("PublicSuper" /* is CONSISTENT */)
    private int Private40;     

    @InRegion("PrivateSuper" /* is UNASSOCIATED */)
    int Default10;   
    @InRegion("DefaultSuper" /* is CONSISTENT */)
    int Default20;   
    @InRegion("ProtectedSuper" /* is CONSISTENT */)
    int Default30; 
    @InRegion("PublicSuper" /* is CONSISTENT */)
    int Default40;    

    @InRegion("PrivateSuper" /* is UNASSOCIATED */)
    protected int Protected10;   
    @InRegion("DefaultSuper" /* is UNASSOCIATED */)
    protected int Protected20;   
    @InRegion("ProtectedSuper" /* is CONSISTENT */)
    protected int Protected30; 
    @InRegion("PublicSuper" /* is CONSISTENT */)
    protected int Protected40;    

    @InRegion("PrivateSuper" /* is UNASSOCIATED */)
    public int Public10;    
    @InRegion("DefaultSuper" /* is UNASSOCIATED */)
    public int Public20;    
    @InRegion("ProtectedSuper" /* is UNASSOCIATED */)
    public int Public30;  
    @InRegion("PublicSuper" /* is CONSISTENT */)
    public int Public40;     
    
    // Local static
    
    @SuppressWarnings("unused")
    @InRegion("PrivateStaticLocal" /* is CONSISTENT */)
    private static int Private100;    
    @SuppressWarnings("unused")
    @InRegion("DefaultStaticLocal" /* is CONSISTENT */)
    private static int Private200;    
    @SuppressWarnings("unused")
    @InRegion("ProtectedStaticLocal" /* is CONSISTENT */)
    private static int Private300;  
    @SuppressWarnings("unused")
    @InRegion("PublicStaticLocal" /* is CONSISTENT */)
    private static int Private400;     

    @InRegion("PrivateStaticLocal" /* is UNASSOCIATED */)
    static int Default100;   
    @InRegion("DefaultStaticLocal" /* is CONSISTENT */)
    static int Default200;   
    @InRegion("ProtectedStaticLocal" /* is CONSISTENT */)
    static int Default300; 
    @InRegion("PublicStaticLocal" /* is CONSISTENT */)
    static int Default400;    

    @InRegion("PrivateStaticLocal" /* is UNASSOCIATED */)
    protected static int Protected100;   
    @InRegion("DefaultStaticLocal" /* is UNASSOCIATED */)
    protected static int Protected200;   
    @InRegion("ProtectedStaticLocal" /* is CONSISTENT */)
    protected static int Protected300; 
    @InRegion("PublicStaticLocal" /* is CONSISTENT */)
    protected static int Protected400;    

    @InRegion("PrivateStaticLocal" /* is UNASSOCIATED */)
    public static int Public100;    
    @InRegion("DefaultStaticLocal" /* is UNASSOCIATED */)
    public static int Public200;    
    @InRegion("ProtectedStaticLocal" /* is UNASSOCIATED */)
    public static int Public300;  
    @InRegion("PublicStaticLocal" /* is CONSISTENT */)
    public static int Public400;     
    
    // Super static
    
    @SuppressWarnings("unused")
    @InRegion("PrivateStaticSuper" /* is CONSISTENT */)
    private static int Private1000;    
    @SuppressWarnings("unused")
    @InRegion("DefaultStaticSuper" /* is CONSISTENT */)
    private static int Private2000;    
    @SuppressWarnings("unused")
    @InRegion("ProtectedStaticSuper" /* is CONSISTENT */)
    private static int Private3000;  
    @SuppressWarnings("unused")
    @InRegion("PublicStaticSuper" /* is CONSISTENT */)
    private static int Private4000;     

    @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
    static int Default1000;   
    @InRegion("DefaultStaticSuper" /* is CONSISTENT */)
    static int Default2000;   
    @InRegion("ProtectedStaticSuper" /* is CONSISTENT */)
    static int Default3000; 
    @InRegion("PublicStaticSuper" /* is CONSISTENT */)
    static int Default4000;    

    @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
    protected static int Protected1000;   
    @InRegion("DefaultStaticSuper" /* is UNASSOCIATED */)
    protected static int Protected2000;   
    @InRegion("ProtectedStaticSuper" /* is CONSISTENT */)
    protected static int Protected3000; 
    @InRegion("PublicStaticSuper" /* is CONSISTENT */)
    protected static int Protected4000;    

    @InRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
    public static int Public1000;    
    @InRegion("DefaultStaticSuper" /* is UNASSOCIATED */)
    public static int Public2000;    
    @InRegion("ProtectedStaticSuper" /* is UNASSOCIATED */)
    public static int Public3000;  
    @InRegion("PublicStaticSuper" /* is CONSISTENT */)
    public static int Public4000;     
  }
}
