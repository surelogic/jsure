package region.nonFieldParent;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
  @Region("R1 extends LocalRegion" /* is CONSISTENT */), // GOOD: Parent is abstract
  @Region("R2 extends field" /* is UNASSOCIATED */), // BAD: Parent is field
  @Region("R3 extends SuperRegion" /* is CONSISTENT */), // GOOD: Parent is abstract
  @Region("R4 extends superField" /* is UNASSOCIATED */) // BAD: Parent is field
})
public class Test extends Super {
  int field;
}
