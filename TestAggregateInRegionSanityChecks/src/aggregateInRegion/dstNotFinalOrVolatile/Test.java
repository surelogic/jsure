package aggregateInRegion.dstNotFinalOrVolatile;

import com.surelogic.AggregateInRegion;
import com.surelogic.Unique;

public class Test {
  /* volatile and final fields cannot have subregions */
  
  private volatile Object volatileField;
  private final Object finalField = null;
  private Object regularField = null;
  
  @Unique
  @AggregateInRegion("finalField" /* is UNASSOCIATED */)
  private final Object aggIntoFinal = new Object();
  
  @Unique
  @AggregateInRegion("volatileField" /* is UNASSOCIATED */)
  private final Object aggIntoVolatile = new Object();
  
  @Unique
  @AggregateInRegion("regularField" /* is CONSISTENT */)
  private final Object aggIntoRegular = new Object();
}
