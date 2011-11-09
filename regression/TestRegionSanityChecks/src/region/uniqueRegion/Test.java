package region.uniqueRegion;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("R1" /* is CONSISTENT */),
  @Region("f1" /* is UNASSOCIATED */), // duplicates field
  @Region("R1" /* is UNASSOCIATED */), // duplicate region
  @Region("SuperRegion" /* is CONSISTENT */) // Okay to shadow from super class
})
public class Test extends Super {
  @SuppressWarnings("unused")
  private int f1;
}
