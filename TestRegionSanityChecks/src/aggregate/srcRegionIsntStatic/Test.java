package aggregate.srcRegionIsntStatic;

import com.surelogic.Aggregate;
import com.surelogic.Unique;

public class Test {
  @Unique
  @Aggregate("Instance into All, InstanceRegion into Instance" /* is CONSISTENT */)
  protected C good1;

  @Unique
  @Aggregate("StaticRegion into All" /* is UNASSOCIATED */)
  protected C bad1;

  @Unique
  @Aggregate("Instance into All, field into Instance" /* is CONSISTENT */)
  protected C good2;

  @Unique
  @Aggregate("staticField into All" /* is UNASSOCIATED */)
  protected C bad2;

  @Unique
  @Aggregate("InstanceRegion into Instance, StaticRegion into All, field into Instance, staticField into All" /* is UNASSOCIATED */)
  protected C allTogether;
  
  
  
  @Unique
  @Aggregate("Instance into All, InstanceRegion into All" /* is CONSISTENT */)
  protected static C goodStatic1;

  @Unique
  @Aggregate("StaticRegion into All" /* is UNASSOCIATED */)
  protected static C badStatic1;

  @Unique
  @Aggregate("Instance into All, field into All" /* is CONSISTENT */)
  protected static C goodStatic2;

  @Unique
  @Aggregate("staticField into All" /* is UNASSOCIATED */)
  protected static C badStatic2;

  @Unique
  @Aggregate("InstanceRegion into All, StaticRegion into All, field into All, staticField into All" /* is UNASSOCIATED */)
  protected static C allTogetherStatic;
}
