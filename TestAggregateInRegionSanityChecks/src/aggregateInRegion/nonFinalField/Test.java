package aggregateInRegion.nonFinalField;

import com.surelogic.AggregateInRegion;
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
  @AggregateInRegion("good1" /* is CONSISTENT */)
  protected Object good1;

  // GOOD
  @Unique
  @InRegion("C")
  @AggregateInRegion("S3" /* is CONSISTENT */)
  protected Object good1a;
  
  // GOOD
  @Unique
  @InRegion("C")
  @AggregateInRegion("A" /* is CONSISTENT */)
  protected Object good2;
  
  // GOOD
  @Unique
  @InRegion("C")
  @AggregateInRegion("S1" /* is CONSISTENT */)
  protected Object good2a;

  // BAD
  @Unique
  @InRegion("C")
  @AggregateInRegion("X" /* is UNASSOCIATED */)
  protected Object bad1;




  /* Canonical aggregation
   * GOOD
   */
  @Unique
  @InRegion("S3")
  @AggregateInRegion("goodStatic1" /* is CONSISTENT */)
  protected static Object goodStatic1;

  @Unique
  @InRegion("S3")
  @AggregateInRegion("A" /* is UNASSOCIATED */)
  protected static Object badStatic1;

  // GOOD
  @Unique
  @InRegion("S3")
  @AggregateInRegion("S1" /* is CONSISTENT */)
  protected static Object goodStatic2;

  // BAD
  @Unique
  @InRegion("S3")
  @AggregateInRegion("S10" /* is UNASSOCIATED */)
  protected static Object badStatic3;
}
