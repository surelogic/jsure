package region.noCycles;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("R1" /* is CONSISTENT */),
  @Region("R2 extends R1" /* is CONSISTENT */),
  @Region("R3 extends BAD" /* is UNBOUND */),
  @Region("R4 extends R2" /* is CONSISTENT */),
  @Region("R5 extends R3" /* is UNBOUND */),
  @Region("BAD extends R5" /* is UNBOUND */)
})
public class Test {
}
