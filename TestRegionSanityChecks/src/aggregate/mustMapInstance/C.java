package aggregate.mustMapInstance;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public R1"),
  @Region("public R2")
})
public class C {
  @Borrowed("this")
  public C() {
  	super();
  }
}
