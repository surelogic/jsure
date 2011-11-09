package inRegion.finalFields;

import com.surelogic.InRegion;
import com.surelogic.Region;

@Region("public R")
public class C {
  @SuppressWarnings("unused")
  @InRegion("R" /* is UNASSOCIATED */)
  private final int finalField = 0;

  @SuppressWarnings("unused")
  @InRegion("R" /* is CONSISTENT */)
  private int field;
}
