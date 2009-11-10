package aggregate.srcRegionIsntStatic;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public static StaticRegion"),
  @Region("public InstanceRegion")
})
public class C {
  protected int field;
  
  protected static int staticField;
  
  @Borrowed("this")
  public C() {
  	super();
  }
}
