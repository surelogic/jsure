package aggregate.srcRegionExists;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("public FromD")
public class D extends C {
  protected int fromD;
  // Do nothing
  
  @Borrowed("this")
  public D() {
  	super();
  }
}
