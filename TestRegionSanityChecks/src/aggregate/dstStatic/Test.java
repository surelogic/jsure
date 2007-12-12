package aggregate.dstStatic;

import com.surelogic.Aggregate;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("public Local"),
  @Region("public static LocalStatic")
})
public class Test extends Parent {
  @Unique
  @Aggregate("Instance into Parent" /* is CONSISTENT */)
  protected final C good1 = new C();
  
  @Unique
  @Aggregate("Instance into ParentStatic" /* is CONSISTENT */)
  protected final C good2 = new C();

  @Unique
  @Aggregate("Instance into Local" /* is CONSISTENT */)
  protected final C good3 = new C();
  
  @Unique
  @Aggregate("Instance into LocalStatic" /* is CONSISTENT */)
  protected final C good4 = new C();

  @Unique
  @Aggregate("Instance into All, R1 into Parent, R2 into ParentStatic, R3 into Local, R4 into LocalStatic" /* is CONSISTENT */)
  protected final C allTogether = new C();



  @Unique
  @Aggregate("Instance into Parent" /* is UNASSOCIATED */)
  protected final static C badStatic1 = new C();
  
  @Unique
  @Aggregate("Instance into ParentStatic" /* is CONSISTENT */)
  protected final static C goodStatic2 = new C();

  @Unique
  @Aggregate("Instance into Local" /* is UNASSOCIATED */)
  protected final static C badStatic3 = new C();
  
  @Unique
  @Aggregate("Instance into LocalStatic" /* is CONSISTENT */)
  protected final static C goodStatic4 = new C();

  @Unique
  @Aggregate("R1 into Parent, R2 into ParentStatic, R3 into Local, R4 into LocalStatic" /* is UNASSOCIATED */)
  protected final static C allTogetherStatic = new C();
}

