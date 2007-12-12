package aggregate.srcRepeated;

import com.surelogic.Aggregate;
import com.surelogic.Unique;

public class Test {
  @Unique
  @Aggregate("Instance into All, R1 into Instance, R2 into All" /* is CONSISTENT */)
  protected C good1;

  @Unique
  @Aggregate("Instance into All, R2 into Instance, R3 into All" /* is CONSISTENT */)
  protected C good2;
  
  @Unique
  @Aggregate("R1 into Instance, R2 into All, R1 into Instance" /* is UNASSOCIATED: R1 used twice */)
  protected C bad;
  
  @Unique
  @Aggregate("Instance into All, f1 into Instance, f2 into All" /* is CONSISTENT */)
  protected C good10;

  @Unique
  @Aggregate("Instance into All, f2 into Instance, f3 into All" /* is CONSISTENT */)
  protected C good20;
  
  @Unique
  @Aggregate("f1 into Instance, f2 into All, f1 into Instance" /* is UNASSOCIATED: f1 used twice */)
  protected C bad0;
  


  @Unique
  @Aggregate("Instance into All, R1 into All, R2 into All" /* is CONSISTENT */)
  protected static C goodStatic1;

  @Unique
  @Aggregate("Instance into All, R2 into All, R3 into All" /* is CONSISTENT */)
  protected static C goodStatic2;
  
  @Unique
  @Aggregate("R1 into All, R2 into All, R1 into All" /* is UNASSOCIATED: R1 used twice */)
  protected static C badStatic;

  @Unique
  @Aggregate("Instance into All, f1 into All, f2 into All" /* is CONSISTENT */)
  protected static C goodStatic10;

  @Unique
  @Aggregate("Instance into All, f2 into All, f3 into All" /* is CONSISTENT */)
  protected static C goodStatic20;
  
  @Unique
  @Aggregate("f1 into All, f2 into All, f1 into All" /* is UNASSOCIATED: f1 used twice */)
  protected static C badStatic0;
}
