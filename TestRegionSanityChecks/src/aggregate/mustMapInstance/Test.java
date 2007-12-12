package aggregate.mustMapInstance;

import com.surelogic.Aggregate;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("public Local1"),
  @Region("public Local2"),
  
  @Region("public static Static1"),
  @Region("public static Static2")
})
public class Test {
  @Unique
  @Aggregate("Instance into Instance" /* is CONSISTENT */)
  protected final C good1 = new C();
  
  @Unique
  @Aggregate("Instance into Instance, R1 into Local1" /* is CONSISTENT */)
  protected final C good2 = new C();
  
  @Unique
  @Aggregate("Instance into Instance, R2 into Local2" /* is CONSISTENT */)
  protected final C good3 = new C();
  
  @Unique
  @Aggregate("Instance into Instance, R1 into Local1, R2 into Local2" /* is CONSISTENT */)
  protected final C good4 = new C();

  @Unique
  @Aggregate("R1 into Instance" /* is UNASSOCIATED */)
  protected final C bad1 = new C();

  @Unique
  @Aggregate("R2 into Instance" /* is UNASSOCIATED */)
  protected final C bad2 = new C();

  @Unique
  @Aggregate("R1 into Local1, R2 into Local2" /* is UNASSOCIATED */)
  protected final C bad3 = new C();

  
  
  @Unique
  @Aggregate("Instance into Instance" /* is CONSISTENT */)
  protected final D good5 = new D();
  
  @Unique
  @Aggregate("Instance into Instance, f1 into Local1" /* is CONSISTENT */)
  protected final D good6 = new D();
  
  @Unique
  @Aggregate("Instance into Instance, f2 into Local2" /* is CONSISTENT */)
  protected final D good7 = new D();
  
  @Unique
  @Aggregate("Instance into Instance, f1 into Local1, f2 into Local2" /* is CONSISTENT */)
  protected final D good8 = new D();

  @Unique
  @Aggregate("f1 into Instance" /* is UNASSOCIATED */)
  protected final D bad4 = new D();

  @Unique
  @Aggregate("f2 into Instance" /* is UNASSOCIATED */)
  protected final D bad5 = new D();

  @Unique
  @Aggregate("f1 into Local1, f2 into Local2" /* is UNASSOCIATED */)
  protected final D bad6 = new D();

  
  
  @Unique
  @Aggregate("Instance into All" /* is CONSISTENT */)
  protected final static C staticGood10 = new C();
  
  @Unique
  @Aggregate("Instance into All, R1 into Static1" /* is CONSISTENT */)
  protected final static C staticGood20 = new C();
  
  @Unique
  @Aggregate("Instance into All, R2 into Static2" /* is CONSISTENT */)
  protected final static C staticGood30 = new C();
  
  @Unique
  @Aggregate("Instance into All, R1 into Static1, R2 into Static2" /* is CONSISTENT */)
  protected final static C staticGood40 = new C();

  @Unique
  @Aggregate("R1 into All" /* is UNASSOCIATED */)
  protected final static C staticBad10 = new C();

  @Unique
  @Aggregate("R2 into All" /* is UNASSOCIATED */)
  protected final static C staticBad20 = new C();

  @Unique
  @Aggregate("R1 into Static1, R2 into Static2" /* is UNASSOCIATED */)
  protected final static C staticBad30 = new C();

  
  
  @Unique
  @Aggregate("Instance into All" /* is CONSISTENT */)
  protected final static D staticGood50 = new D();
  
  @Unique
  @Aggregate("Instance into All, f1 into Static1" /* is CONSISTENT */)
  protected final static D staticGood60 = new D();
  
  @Unique
  @Aggregate("Instance into All, f2 into Static2" /* is CONSISTENT */)
  protected final static D staticGood70 = new D();
  
  @Unique
  @Aggregate("Instance into All, f1 into Static1, f2 into Static2" /* is CONSISTENT */)
  protected final static D staticGood80 = new D();

  @Unique
  @Aggregate("f1 into All" /* is UNASSOCIATED */)
  protected final static D staticBad40 = new D();

  @Unique
  @Aggregate("f2 into All" /* is UNASSOCIATED */)
  protected final static D staticBad50 = new D();

  @Unique
  @Aggregate("f1 into Static1, f2 into Static2" /* is UNASSOCIATED */)
  protected final static D staticBad60 = new D();
}

