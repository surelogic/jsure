package aggregateInRegion.dstStatic;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public R1"),
  @Region("public R2"),
  @Region("public R3"),
  @Region("public R4")
})
public class C {
  @com.surelogic.Unique("return") // don't use import because I don't want to change the line numbers of the region declarations
  public C() {
  	super();
  }
}
