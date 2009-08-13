package test;

import com.surelogic.Aggregate;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  // cannot have a final region as a parent
  @Region("private R extends finalField" /* is UNASSOCIATED */),
  
  // cannot have a volatile region as a parent
  @Region("private A extends volatileField" /* is UNASSOCIATED */),
  
  @Region("private B extends regularField" /* is CONSISTENT */),
  
  /* Cannot have a cycle of abstract regions because the binder won't bind
   * forward references to abstract region names.  Can have a cycle if a field
   * is involved.  Error is flagged on the @InRegion annotation that closes the 
   * cycle.
   */
  @Region("private ZZZ extends topField" /* is CONSISTENT */)
})
@SuppressWarnings("unused")
public class Test {
  /* A volatile field cannot be unique */
  
  @Unique( /* is UNASSOCIATED */)
  private volatile Object uniqueVolatile;
  
  @Unique( /* is CONSISTENT */)
  private Object goodUnique1;
  
  @Unique( /* is CONSISTENT */)
  private final Object goodUnique2 = new Object();
  
  
  
  /* volatile and final fields cannot have subregions */
  
  private volatile Object volatileField;
  private final Object finalField = null;
  private Object regularField = null;
  
  @InRegion("volatileField" /* is UNASSOCIATED */)
  private int x;
  
  @InRegion("finalField" /* is UNASSOCIATED */)
  private int y;
  
  @InRegion("regularField" /* is CONSISTENT */)
  private int z;
  
  
  /* Cannot aggregate into a final or volatile field */
  
  @Unique
  @Aggregate("Instance into finalField" /* is UNASSOCIATED */)
  private final Object aggIntoFinal = new Object();
  
  @Unique
  @Aggregate("Instance into volatileField" /* is UNASSOCIATED */)
  private final Object aggIntoVolatile = new Object();
  
  @Unique
  @Aggregate("Instance into regularField" /* is CONSISTENT */)
  private final Object aggIntoRegular = new Object();
  
  
  /* Cycle checking */
  
  // Create a cycle with an abstract region
  @InRegion("ZZZ" /* is UNASSOCIATED */)
  private int topField;
  
  // Create a cycle of two fields
  @InRegion("otherOne" /* is CONSISTENT */)
  private int thisOne;
  
  @InRegion("thisOne" /* is UNASSOCIATED */)
  private int otherOne;
}
