package region.nonFieldParent;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
  @Region("R1 extends LocalRegion" /* is CONSISTENT */), // GOOD: Parent is abstract
  @Region("R2 extends field" /* is CONSISTENT */), // GOOD: Parent is field (Good as of 2009-08-13)
  @Region("R3 extends SuperRegion" /* is CONSISTENT */), // GOOD: Parent is abstract
  @Region("R4 extends superField" /* is CONSISTENT */) // BAD: Parent is field (Good as of 2009-08-13)
})
public class Test extends Super {
  int field;
}
