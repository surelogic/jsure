package aggregate.srcRegionExists;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("public FromC")
public class C {
  protected int fromC;
  // Do nothing
  
  @Borrowed("this")
  public C() {
  	super();
  }
}
