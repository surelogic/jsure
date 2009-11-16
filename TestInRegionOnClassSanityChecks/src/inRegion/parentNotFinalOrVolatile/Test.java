package inRegion.parentNotFinalOrVolatile;

import com.surelogic.Aggregate;
import com.surelogic.InRegion;
import com.surelogic.InRegions;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@SuppressWarnings("unused")
@InRegions({
	@InRegion("x into volatileField" /* is UNASSOCIATED */),
	@InRegion("y into finalField" /* is UNASSOCIATED */),
	@InRegion("z into regularField" /* is CONSISTENT */)
})
public class Test {
  /* volatile and final fields cannot have subregions */
  
  private volatile Object volatileField;
  private final Object finalField = null;
  private Object regularField = null;
  
  private int x;
  private int y;
  private int z;
}
