package aggregate.srcRegionExists;

import com.surelogic.Aggregate;
import com.surelogic.Unique;

public class Test {
  @Unique
  @Aggregate("Instance into All, FromC into Instance" /* is CONSISTENT */)
  protected C good1;
  
  @Unique
  @Aggregate("FromD into Instance" /* is UNBOUND: Region from subclass */)
  protected C bad1;
  
  @Unique
  @Aggregate("FromOther into Instance" /* is UNBOUND: Region from unrelated class */)
  protected C bad2;
  
  @Unique
  @Aggregate("NoSuchRegion into Instance" /* is UNBOUND: No such region */)
  protected C bad3;

  @Unique
  @Aggregate("Instance into All, fromC into Instance" /* is CONSISTENT */)
  protected C good10;
  
  @Unique
  @Aggregate("fromD into Instance" /* is UNBOUND: Region from subclass */)
  protected C bad10;
  
  @Unique
  @Aggregate("fromOther into Instance" /* is UNBOUND: Region from unrelated class */)
  protected C bad20;



  @Unique
  @Aggregate("FromC into Instance, FromD into Instance, FromOther into Instance, NoSuchRegion into Instance, fromC into Instance, fromD into Instance, fromOther into Instance" /* is UNBOUND: No such region */)
  protected C allTogether;



  @Unique
  @Aggregate("Instance into All, FromC into All" /* is CONSISTENT */)
  protected static C goodStatic1;
  
  @Unique
  @Aggregate("FromD into All" /* is UNBOUND: Region from subclass */)
  protected static C badStatic1;
  
  @Unique
  @Aggregate("FromOther into All" /* is UNBOUND: Region from unrelated class */)
  protected static C badStatic2;
  
  @Unique
  @Aggregate("NoSuchRegion into All" /* is UNBOUND: No such region */)
  protected static C badStatic3;

  @Unique
  @Aggregate("Instance into All, fromC into All" /* is CONSISTENT */)
  protected static C goodStatic10;
  
  @Unique
  @Aggregate("fromD into All" /* is UNBOUND: Region from subclass */)
  protected static C badStatic10;
  
  @Unique
  @Aggregate("fromOther into All" /* is UNBOUND: Region from unrelated class */)
  protected static C badStatic20;



  @Unique
  @Aggregate("FromC into All, FromD into All, FromOther into All, NoSuchRegion into All, fromC into All, fromD into All, fromOther into All" /* is UNBOUND: No such region */)
  protected static C allTogetherStatic;
}
