package region.staticParents;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("static LocalStatic"),
  @Region("LocalInstance"),
  @Region("R1 extends SuperStatic" /* is CONSISTENT */), // OKAY
  @Region("R2 extends SuperInstance" /* is CONSISTENT */), // OKAY
  @Region("R3 extends LocalStatic" /* is CONSISTENT */), // OKAY
  @Region("R4 extends LocalInstance" /* is CONSISTENT */), // OKAY
  @Region("static S1 extends SuperStatic" /* is CONSISTENT */), // OKAY
  @Region("static S2 extends SuperInstance" /* is UNASSOCIATED */), // BAD
  @Region("static S3 extends LocalStatic" /* is CONSISTENT */), // OKAY
  @Region("static S4 extends LocalInstance" /* is UNASSOCIATED */), // BAD
})
public class Test extends Super {
}
