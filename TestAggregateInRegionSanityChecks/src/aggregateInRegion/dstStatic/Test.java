package aggregateInRegion.dstStatic;

import com.surelogic.AggregateInRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("public Local"),
  @Region("public static LocalStatic")
})
public class Test extends Parent {
  @Unique
  @AggregateInRegion("Parent" /* is CONSISTENT */)
  protected final C good1 = new C();
  
  @Unique
  @AggregateInRegion("ParentStatic" /* is CONSISTENT */)
  protected final C good2 = new C();

  @Unique
  @AggregateInRegion("Local" /* is CONSISTENT */)
  protected final C good3 = new C();
  
  @Unique
  @AggregateInRegion("LocalStatic" /* is CONSISTENT */)
  protected final C good4 = new C();



  @Unique
  @AggregateInRegion("Parent" /* is UNASSOCIATED */)
  protected final static C badStatic1 = new C();
  
  @Unique
  @AggregateInRegion("ParentStatic" /* is CONSISTENT */)
  protected final static C goodStatic2 = new C();

  @Unique
  @AggregateInRegion("Local" /* is UNASSOCIATED */)
  protected final static C badStatic3 = new C();
  
  @Unique
  @AggregateInRegion("LocalStatic" /* is CONSISTENT */)
  protected final static C goodStatic4 = new C();
}

