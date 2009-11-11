package aggregateInRegion.dstRegionExists;

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
  @AggregateInRegion("Child" /* is UNBOUND */)
  protected final C bad1 = new C();

  @Unique
  @AggregateInRegion("ChildStatic" /* is UNBOUND */)
  protected final C bad2 = new C();

  @Unique
  @AggregateInRegion("Other" /* is UNBOUND */)
  protected final C bad3 = new C();

  @Unique
  @AggregateInRegion("OtherStatic" /* is UNBOUND */)
  protected final C bad4 = new C();



  @Unique
  @AggregateInRegion("ParentStatic" /* is CONSISTENT */)
  protected final static C staticGood2 = new C();

  @Unique
  @AggregateInRegion("LocalStatic" /* is CONSISTENT */)
  protected final static C staticGood4 = new C();
  
  @Unique
  @AggregateInRegion("ChildStatic" /* is UNBOUND */)
  protected final static C staticBad2 = new C();

  @Unique
  @AggregateInRegion("OtherStatic" /* is UNBOUND */)
  protected final static C staticBad4 = new C();
}

