package inRegion.noCycles;

import com.surelogic.Aggregate;
import com.surelogic.InRegion;
import com.surelogic.InRegions;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

/* Cannot have a cycle of abstract regions because the binder won't bind
 * forward references to abstract region names.  Can have a cycle if a field
 * is involved.  Error is flagged on the @InRegion annotation that closes the 
 * cycle.
 */
@Region("private ZZZ extends topField" /* is CONSISTENT */)
@InRegions({
	@InRegion("topField into ZZZ" /* is UNASSOCIATED */),
	
	@InRegion("thisOne into otherOne" /* is CONSISTENT */),
	@InRegion("otherOne into thisOne" /* is UNASSOCIATED */)
})
@SuppressWarnings("unused")
public class Test {
  private int topField;
  
  private int thisOne;
  private int otherOne;
}
