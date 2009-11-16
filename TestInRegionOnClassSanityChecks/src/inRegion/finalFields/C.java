package inRegion.finalFields;

import com.surelogic.InRegion;
import com.surelogic.InRegions;
import com.surelogic.Region;

@Region("public R")
@InRegions({
	@InRegion("finalField into R" /* is UNASSOCIATED */),
	@InRegion("field into R" /* is CONSISTENT */)
})
public class C {
  @SuppressWarnings("unused")
  private final int finalField = 0;

  @SuppressWarnings("unused")
  private int field;
}
