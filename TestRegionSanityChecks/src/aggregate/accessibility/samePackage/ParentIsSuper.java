package aggregate.accessibility.samePackage;

import com.surelogic.Aggregate;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

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
public class ParentIsSuper extends Super {
  
  @Region("private PrivateInnerAgg")
  private final class PrivateInnerDelegate {
  }
     
  
  
  // ==== Private Source Region ====
  
  // ------ Local Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into PrivateLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate private1 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into DefaultLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate private2 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into ProtectedLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate private3 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into PublicLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate private4 = new PrivateDelegate();
  
  // ------ Local Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into PrivateStaticLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate privateStatic1 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into DefaultStaticLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate privateStatic2 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into ProtectedStaticLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate privateStatic3 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into PublicStaticLocal" /* is UNASSOCIATED */)
  private final PrivateDelegate privateStatic4 = new PrivateDelegate();
  
  // ------ Super Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into PrivateSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuper1 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into DefaultSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuper2 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into ProtectedSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuper3 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateAgg into PublicSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuper4 = new PrivateDelegate();
  
  // ------ Super Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into PrivateStaticSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuperStatic1 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into DefaultStaticSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuperStatic2 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into ProtectedStaticSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuperStatic3 = new PrivateDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateAgg into PublicStaticSuper" /* is UNASSOCIATED */)
  private final PrivateDelegate privateSuperStatic4 = new PrivateDelegate();

  //
  
  
  // ------ Local Instance Destination Region, Inner Delegate ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into PrivateLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInner1 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into DefaultLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInner2 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into ProtectedLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInner3 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into PublicLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInner4 = new PrivateInnerDelegate();
  
  // ------ Local Static Destination Region, Inner Delegate ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into PrivateStaticLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerStatic1 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into DefaultStaticLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerStatic2 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into ProtectedStaticLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerStatic3 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into PublicStaticLocal" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerStatic4 = new PrivateInnerDelegate();
  
  // ------ Super Instance Destination Region, Inner Delegate ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into PrivateSuper" /* is UNASSOCIATED */)
  private final PrivateInnerDelegate privateInnerSuper1 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into DefaultSuper" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerSuper2 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into ProtectedSuper" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerSuper3 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PrivateInnerAgg into PublicSuper" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerSuper4 = new PrivateInnerDelegate();
  
  // ------ Super Static Destination Region, Inner Delegate ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into PrivateStaticSuper" /* is UNASSOCIATED */)
  private final PrivateInnerDelegate privateInnerSuperStatic1 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into DefaultStaticSuper" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerSuperStatic2 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into ProtectedStaticSuper" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerSuperStatic3 = new PrivateInnerDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PrivateInnerAgg into PublicStaticSuper" /* is CONSISTENT */)
  private final PrivateInnerDelegate privateInnerSuperStatic4 = new PrivateInnerDelegate();

  

  // ==== Default Source Region ====
  
  // ------ Local Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into PrivateLocal" /* is CONSISTENT */)
  private final DefaultDelegate default1 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into DefaultLocal" /* is CONSISTENT */)
  private final DefaultDelegate default2 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into ProtectedLocal" /* is CONSISTENT */)
  private final DefaultDelegate default3 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into PublicLocal" /* is CONSISTENT */)
  private final DefaultDelegate default4 = new DefaultDelegate();
  
  // ------ Local Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into PrivateStaticLocal" /* is CONSISTENT */)
  private final DefaultDelegate defaultStatic1 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into DefaultStaticLocal" /* is CONSISTENT */)
  private final DefaultDelegate defaultStatic2 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into ProtectedStaticLocal" /* is CONSISTENT */)
  private final DefaultDelegate defaultStatic3 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into PublicStaticLocal" /* is CONSISTENT */)
  private final DefaultDelegate defaultStatic4 = new DefaultDelegate();
  
  // ------ Super Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into PrivateSuper" /* is UNASSOCIATED */)
  private final DefaultDelegate defaultSuper1 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into DefaultSuper" /* is CONSISTENT */)
  private final DefaultDelegate defaultSuper2 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into ProtectedSuper" /* is CONSISTENT */)
  private final DefaultDelegate defaultSuper3 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, DefaultAgg into PublicSuper" /* is CONSISTENT */)
  private final DefaultDelegate defaultSuper4 = new DefaultDelegate();
  
  // ------ Super Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into PrivateStaticSuper" /* is UNASSOCIATED */)
  private final DefaultDelegate defaultSuperStatic1 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into DefaultStaticSuper" /* is CONSISTENT */)
  private final DefaultDelegate defaultSuperStatic2 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into ProtectedStaticSuper" /* is CONSISTENT */)
  private final DefaultDelegate defaultSuperStatic3 = new DefaultDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, DefaultAgg into PublicStaticSuper" /* is CONSISTENT */)
  private final DefaultDelegate defaultSuperStatic4 = new DefaultDelegate();

  

  // ==== Protected Source Region ====
  
  // ------ Local Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into PrivateLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protected1 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into DefaultLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protected2 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into ProtectedLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protected3 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into PublicLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protected4 = new ProtectedDelegate();
  
  // ------ Local Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into PrivateStaticLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protectedStatic1 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into DefaultStaticLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protectedStatic2 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into ProtectedStaticLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protectedStatic3 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into PublicStaticLocal" /* is CONSISTENT */)
  private final ProtectedDelegate protectedStatic4 = new ProtectedDelegate();
  
  // ------ Super Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into PrivateSuper" /* is UNASSOCIATED */)
  private final ProtectedDelegate protectedSuper1 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into DefaultSuper" /* is CONSISTENT */)
  private final ProtectedDelegate protectedSuper2 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into ProtectedSuper" /* is CONSISTENT */)
  private final ProtectedDelegate protectedSuper3 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, ProtectedAgg into PublicSuper" /* is CONSISTENT */)
  private final ProtectedDelegate protectedSuper4 = new ProtectedDelegate();
  
  // ------ Super Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into PrivateStaticSuper" /* is UNASSOCIATED */)
  private final ProtectedDelegate protectedSuperStatic1 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into DefaultStaticSuper" /* is CONSISTENT */)
  private final ProtectedDelegate protectedSuperStatic2 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into ProtectedStaticSuper" /* is CONSISTENT */)
  private final ProtectedDelegate protectedSuperStatic3 = new ProtectedDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, ProtectedAgg into PublicStaticSuper" /* is CONSISTENT */)
  private final ProtectedDelegate protectedSuperStatic4 = new ProtectedDelegate();

  

  // ==== Public Source Region ====
  
  // ------ Local Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into PrivateLocal" /* is CONSISTENT */)
  private final PublicDelegate public1 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into DefaultLocal" /* is CONSISTENT */)
  private final PublicDelegate public2 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into ProtectedLocal" /* is CONSISTENT */)
  private final PublicDelegate public3 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into PublicLocal" /* is CONSISTENT */)
  private final PublicDelegate public4 = new PublicDelegate();
  
  // ------ Local Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into PrivateStaticLocal" /* is CONSISTENT */)
  private final PublicDelegate publicStatic1 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into DefaultStaticLocal" /* is CONSISTENT */)
  private final PublicDelegate publicStatic2 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into ProtectedStaticLocal" /* is CONSISTENT */)
  private final PublicDelegate publicStatic3 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into PublicStaticLocal" /* is CONSISTENT */)
  private final PublicDelegate publicStatic4 = new PublicDelegate();
  
  // ------ Local Instance Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into PrivateSuper" /* is UNASSOCIATED */)
  private final PublicDelegate publicSuper1 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into DefaultSuper" /* is CONSISTENT */)
  private final PublicDelegate publicSuper2 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into ProtectedSuper" /* is CONSISTENT */)
  private final PublicDelegate publicSuper3 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into Instance, PublicAgg into PublicSuper" /* is CONSISTENT */)
  private final PublicDelegate publicSuper4 = new PublicDelegate();
  
  // ------ Super Static Destination Region ------
  
  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into PrivateStaticSuper" /* is UNASSOCIATED */)
  private final PublicDelegate publicSuperStatic1 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into DefaultStaticSuper" /* is CONSISTENT */)
  private final PublicDelegate publicSuperStatic2 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into ProtectedStaticSuper" /* is CONSISTENT */)
  private final PublicDelegate publicSuperStatic3 = new PublicDelegate();

  @SuppressWarnings("unused")
  @Unique
  @Aggregate("Instance into All, PublicAgg into PublicStaticSuper" /* is CONSISTENT */)
  private final PublicDelegate publicSuperStatic4 = new PublicDelegate();
}
