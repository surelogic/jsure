package aggregate.srcRepeated;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public R1"),
  @Region("public R2"),
  @Region("public R3")
})
public class C {
  protected int f1;
  protected int f2;
  protected int f3;
  // Do nothing
  
  @Borrowed("this")
  public C() {
  	super();
  }
}
