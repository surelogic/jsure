package aggregate.nonFinalField;

import com.surelogic.Aggregate;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

/* Non final fields must be aggregated into regions that include the field. */
@Regions({
  @Region("public static S1"),
  @Region("public static S2 extends S1"),
  @Region("public static S3 extends S2"),
  
  @Region("public static S10"),
  @Region("public static S20 extends S10"),
  @Region("public static S30 extends S20"),

  @Region("public A extends S3"),
  @Region("public B extends A"),
  @Region("public C extends B"),
  
  @Region("public X extends S30"),
  @Region("public Y extends X"),
  @Region("public Z extends Y")
})
public class Test {
  /* Canonical aggregation
   * GOOD
   */
  @Unique
  @InRegion("C")
  @Aggregate("Instance into good1" /* is CONSISTENT */)
  protected C good1;

  // GOOD
  @Unique
  @InRegion("C")
  @Aggregate("Instance into S3" /* is CONSISTENT */)
  protected C good1a;
  
  // GOOD
  @Unique
  @InRegion("C")
  @Aggregate("Instance into A, R1 into B, R2 into C" /* is CONSISTENT */)
  protected C good2;
  
  // GOOD
  @Unique
  @InRegion("C")
  @Aggregate("Instance into S1, R1 into B, R2 into C, R3 into S2, R4 into good2a" /* is CONSISTENT */)
  protected C good2a;

  // BAD
  @Unique
  @InRegion("C")
  @Aggregate("Instance into X, R1 into Y, R2 into Z" /* is UNASSOCIATED */)
  protected C bad1;




  /* Canonical aggregation
   * GOOD
   */
  @Unique
  @InRegion("S3")
  @Aggregate("Instance into goodStatic1" /* is CONSISTENT */)
  protected static C goodStatic1;

  @Unique
  @InRegion("S3")
  @Aggregate("Instance into A" /* is UNASSOCIATED */)
  protected static C badStatic1;

  // GOOD
  @Unique
  @InRegion("S3")
  @Aggregate("Instance into S1, R1 into S2, R2 into S3" /* is CONSISTENT */)
  protected static C goodStatic2;
  
  // GOOD
  @Unique
  @InRegion("S3")
  @Aggregate("Instance into S1, R1 into B, R2 into C, R3 into S2, R4 into S3" /* is UNASSOCIATED */)
  protected static C badStatic2;

  // BAD
  @Unique
  @InRegion("S3")
  @Aggregate("Instance into S10, R1 into S20, R2 into S30" /* is UNASSOCIATED */)
  protected static C badStatic3;
}
