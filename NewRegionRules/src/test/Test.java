package test;

import com.surelogic.Aggregate;
import com.surelogic.InRegion;
import com.surelogic.Unique;

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
}
