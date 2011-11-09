package region.parentMustExist;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
  @Region("R1 extends NoSuchRegion" /* is UNBOUND */), // BAD: Parent doesn't exist
  @Region("R2 extends Instance" /* is CONSISTENT */), // GOOD: Instance exists
  @Region("R3 extends SuperRegion" /* is CONSISTENT */), // GOOD: Region from parent
  @Region("R4 extends ChildRegion" /* is UNBOUND */), // BAD: Region from child
  @Region("R5 extends UnrelatedRegion" /* is UNBOUND */), // BAD: Region from unrelated class
  @Region("R6 extends LocalRegion" /* is CONSISTENT */) // GOOD: Region from same class
})
public class Test extends Super {
}
