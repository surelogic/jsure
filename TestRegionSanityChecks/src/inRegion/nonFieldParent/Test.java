package inRegion.nonFieldParent;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
})
public class Test extends Super {
  int field;
  
  @InRegion("LocalRegion" /* is CONSISTENT */) // Good
  int field1;
  
  @InRegion("field" /* is CONSISTENT */) // Good (as of 2009-08-11)
  int field2;
  
  @InRegion("SuperRegion" /* is CONSISTENT */) // Good
  int field3;
  
  @InRegion("superField" /* is CONSISTENT */) // Good (as of 2009-08-11)
  int field4;
}
