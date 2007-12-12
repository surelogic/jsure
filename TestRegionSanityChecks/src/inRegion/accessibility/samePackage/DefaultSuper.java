package inRegion.accessibility.samePackage;

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
class DefaultSuper {
  @Regions({
    @Region("private PrivateLocal"),
    @Region("DefaultLocal"),
    @Region("protected ProtectedLocal"),
    @Region("public PublicLocal"),
    @Region("private static PrivateStaticLocal"),
    @Region("static DefaultStaticLocal"),
    @Region("protected static ProtectedStaticLocal"),
    @Region("public static PublicStaticLocal"),
    
    // Local instance
    
    @Region("private Private1 extends PrivateLocal" /* is CONSISTENT */),    // GOOD
    @Region("private Private2 extends DefaultLocal" /* is CONSISTENT */),    // GOOD
    @Region("private Private3 extends ProtectedLocal" /* is CONSISTENT */),  // GOOD
    @Region("private Private4 extends PublicLocal" /* is CONSISTENT */),     // GOOD

    @Region("Default1 extends PrivateLocal" /* is UNASSOCIATED */),   // BAD
    @Region("Default2 extends DefaultLocal" /* is CONSISTENT */),   // GOOD
    @Region("Default3 extends ProtectedLocal" /* is CONSISTENT */), // GOOD
    @Region("Default4 extends PublicLocal" /* is CONSISTENT */),    // GOOD

    @Region("protected Protected1 extends PrivateLocal" /* is UNASSOCIATED */),   // BAD
    @Region("protected Protected2 extends DefaultLocal" /* is UNASSOCIATED */),   // BAD
    @Region("protected Protected3 extends ProtectedLocal" /* is CONSISTENT */), // GOOD
    @Region("protected Protected4 extends PublicLocal" /* is CONSISTENT */),    // GOOD

    @Region("public Public1 extends PrivateLocal" /* is UNASSOCIATED */),    // BAD
    @Region("public Public2 extends DefaultLocal" /* is UNASSOCIATED */),    // BAD
    @Region("public Public3 extends ProtectedLocal" /* is UNASSOCIATED */),  // BAD
    @Region("public Public4 extends PublicLocal" /* is CONSISTENT */),     // GOOD
    
    // Super instance
    
    @Region("private Private10 extends PrivateSuper" /* is CONSISTENT */),    // GOOD
    @Region("private Private20 extends DefaultSuper" /* is CONSISTENT */),    // GOOD
    @Region("private Private30 extends ProtectedSuper" /* is CONSISTENT */),  // GOOD
    @Region("private Private40 extends PublicSuper" /* is CONSISTENT */),     // GOOD

    @Region("Default10 extends PrivateSuper" /* is UNASSOCIATED */),   // BAD
    @Region("Default20 extends DefaultSuper" /* is CONSISTENT */),   // GOOD
    @Region("Default30 extends ProtectedSuper" /* is CONSISTENT */), // GOOD
    @Region("Default40 extends PublicSuper" /* is CONSISTENT */),    // GOOD

    @Region("protected Protected10 extends PrivateSuper" /* is UNASSOCIATED */),   // BAD
    @Region("protected Protected20 extends DefaultSuper" /* is UNASSOCIATED */),   // BAD
    @Region("protected Protected30 extends ProtectedSuper" /* is CONSISTENT */), // GOOD
    @Region("protected Protected40 extends PublicSuper" /* is CONSISTENT */),    // GOOD

    @Region("public Public10 extends PrivateSuper" /* is UNASSOCIATED */),    // BAD
    @Region("public Public20 extends DefaultSuper" /* is UNASSOCIATED */),    // BAD
    @Region("public Public30 extends ProtectedSuper" /* is UNASSOCIATED */),  // BAD
    @Region("public Public40 extends PublicSuper" /* is CONSISTENT */),     // GOOD
    
    // Local static
    
    @Region("private static Private100 extends PrivateStaticLocal" /* is CONSISTENT */),    // GOOD
    @Region("private static Private200 extends DefaultStaticLocal" /* is CONSISTENT */),    // GOOD
    @Region("private static Private300 extends ProtectedStaticLocal" /* is CONSISTENT */),  // GOOD
    @Region("private static Private400 extends PublicStaticLocal" /* is CONSISTENT */),     // GOOD

    @Region("static Default100 extends PrivateStaticLocal" /* is UNASSOCIATED */),   // BAD
    @Region("static Default200 extends DefaultStaticLocal" /* is CONSISTENT */),   // GOOD
    @Region("static Default300 extends ProtectedStaticLocal" /* is CONSISTENT */), // GOOD
    @Region("static Default400 extends PublicStaticLocal" /* is CONSISTENT */),    // GOOD

    @Region("protected static Protected100 extends PrivateStaticLocal" /* is UNASSOCIATED */),   // BAD
    @Region("protected static Protected200 extends DefaultStaticLocal" /* is UNASSOCIATED */),   // BAD
    @Region("protected static Protected300 extends ProtectedStaticLocal" /* is CONSISTENT */), // GOOD
    @Region("protected static Protected400 extends PublicStaticLocal" /* is CONSISTENT */),    // GOOD

    @Region("public static Public100 extends PrivateStaticLocal" /* is UNASSOCIATED */),    // BAD
    @Region("public static Public200 extends DefaultStaticLocal" /* is UNASSOCIATED */),    // BAD
    @Region("public static Public300 extends ProtectedStaticLocal" /* is UNASSOCIATED */),  // BAD
    @Region("public static Public400 extends PublicStaticLocal" /* is CONSISTENT */),     // GOOD
    
    // Super static
    
    @Region("private static Private1000 extends PrivateStaticSuper" /* is CONSISTENT */),    // GOOD
    @Region("private static Private2000 extends DefaultStaticSuper" /* is CONSISTENT */),    // GOOD
    @Region("private static Private3000 extends ProtectedStaticSuper" /* is CONSISTENT */),  // GOOD
    @Region("private static Private4000 extends PublicStaticSuper" /* is CONSISTENT */),     // GOOD

    @Region("static Default1000 extends PrivateStaticSuper" /* is UNASSOCIATED */),   // BAD
    @Region("static Default2000 extends DefaultStaticSuper" /* is CONSISTENT */),   // GOOD
    @Region("static Default3000 extends ProtectedStaticSuper" /* is CONSISTENT */), // GOOD
    @Region("static Default4000 extends PublicStaticSuper" /* is CONSISTENT */),    // GOOD

    @Region("protected static Protected1000 extends PrivateStaticSuper" /* is UNASSOCIATED */),   // BAD
    @Region("protected static Protected2000 extends DefaultStaticSuper" /* is UNASSOCIATED */),   // BAD
    @Region("protected static Protected3000 extends ProtectedStaticSuper" /* is CONSISTENT */), // GOOD
    @Region("protected static Protected4000 extends PublicStaticSuper" /* is CONSISTENT */),    // GOOD

    @Region("public static Public1000 extends PrivateStaticSuper" /* is UNASSOCIATED */),    // BAD
    @Region("public static Public2000 extends DefaultStaticSuper" /* is UNASSOCIATED */),    // BAD
    @Region("public static Public3000 extends ProtectedStaticSuper" /* is UNASSOCIATED */),  // BAD
    @Region("public static Public4000 extends PublicStaticSuper" /* is CONSISTENT */),     // GOOD
  })
  public class Inner_ParentIsDefaultSuper extends DefaultSuper {
  }
}
