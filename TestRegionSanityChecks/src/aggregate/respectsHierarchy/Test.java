package aggregate.respectsHierarchy;

import com.surelogic.Aggregate;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

/**
 * All
 *   Instance
 *     Instance1
 *       Instance2
 *         Instance4
 *       Instance3
 *   Static1
 *     Static2
 *       Static4
 *     Static3       
 */
@Regions({
  @Region("public Instance1"),
  @Region("public Instance2 extends Instance1"),
  @Region("public Instance3 extends Instance1"),
  @Region("public Instance4 extends Instance2"),

  @Region("public static Static1"),
  @Region("public static Static2 extends Static1"),
  @Region("public static Static3 extends Static1"),
  @Region("public static Static4 extends Static2")
})
public class Test {
  @Unique
  @Aggregate("Instance into Instance" /* is CONSISTENT */)
  protected final C good1 = new C();
  
  @Unique
  @Aggregate("Instance into Instance, inR1 into Instance4" /* is CONSISTENT */)
  protected final C good2 = new C();
  
  @Unique
  @Aggregate("inR1 into Instance4, Instance into Instance" /* is CONSISTENT */)
  protected final C good3 = new C();
  
  @Unique
  @Aggregate("R4 into Instance4, R3 into Instance3, R2 into Instance2, R1 into Instance1, Instance into Instance" /* is CONSISTENT */)
  protected final C good4 = new C();
  
  @Unique
  @Aggregate("R4 into Instance4, R1 into Instance1, Instance into Instance, R3 into Instance3, R2 into Instance2" /* is CONSISTENT */)
  protected final C good5 = new C();

  @Unique
  @Aggregate("Instance into Instance1, R2 into Instance3" /* is CONSISTENT */)
  protected final C good6 = new C();

  @Unique
  @Aggregate("R2 into Instance3, Instance into Instance1" /* is CONSISTENT */)
  protected final C good7 = new C();
  
  
  
  @Unique
  @Aggregate("Instance into Instance, R1 into Instance2, R2 into Instance3" /* is UNASSOCIATED */)
  protected final C bad1 = new C();
  
  @Unique
  @Aggregate("Instance into Instance, R2 into Instance3, R1 into Instance2" /* is UNASSOCIATED */)
  protected final C bad2 = new C();
  
  @Unique
  @Aggregate("Instance into Instance4, R1 into Instance2, R2 into Instance1, R4 into Instance, inR4 into All" /* is UNASSOCIATED */)
  protected final C bad3 = new C();
  
  @Unique
  @Aggregate("R1 into Instance2, Instance into Instance4, R2 into Instance1, inR4 into All, R4 into Instance" /* is UNASSOCIATED */)
  protected final C bad4 = new C();




  @Unique
  @Aggregate("Instance into All" /* is CONSISTENT */)
  protected final C good10 = new C();
  
  @Unique
  @Aggregate("Instance into All, inR1 into Static4" /* is CONSISTENT */)
  protected final C good20 = new C();
  
  @Unique
  @Aggregate("inR1 into Static4, Instance into All" /* is CONSISTENT */)
  protected final C good30 = new C();
  
  @Unique
  @Aggregate("R4 into Static4, R3 into Static3, R2 into Static2, R1 into Static1, Instance into All" /* is CONSISTENT */)
  protected final C good40 = new C();
  
  @Unique
  @Aggregate("R4 into Static4, R1 into Static1, Instance into All, R3 into Static3, R2 into Static2" /* is CONSISTENT */)
  protected final C good50 = new C();

  @Unique
  @Aggregate("Instance into Static1, R2 into Static3" /* is CONSISTENT */)
  protected final C good60 = new C();

  @Unique
  @Aggregate("R2 into Static3, Instance into Static1" /* is CONSISTENT */)
  protected final C good70 = new C();
  
  
  
  @Unique
  @Aggregate("Instance into All, R1 into Static2, R2 into Static3" /* is UNASSOCIATED */)
  protected final C bad10 = new C();
  
  @Unique
  @Aggregate("Instance into All, R2 into Static3, R1 into Static2" /* is UNASSOCIATED */)
  protected final C bad20 = new C();
  
  @Unique
  @Aggregate("Instance into Static4, R1 into Static2, R2 into Static1, R4 into All" /* is UNASSOCIATED */)
  protected final C bad30 = new C();
  
  @Unique
  @Aggregate("R1 into Static2, Instance into Static4, R2 into Static1, R4 into All" /* is UNASSOCIATED */)
  protected final C bad40 = new C();
}  

