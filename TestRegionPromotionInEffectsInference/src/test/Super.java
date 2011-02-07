package test;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public SuperPublic"),
  @Region("protected SuperProtected extends SuperPublic"),
  @Region("SuperDefault extends SuperProtected")
})
public class Super {
  @InRegion("SuperPublic")
  public int publicField;
  
  @InRegion("SuperProtected")
  protected int protectedField;
  
  @InRegion("SuperDefault")
  int defaultField;
}
