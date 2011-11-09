package inRegion.nonFieldParent;

import com.surelogic.InRegion;
import com.surelogic.InRegions;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
})
@InRegions({
	@InRegion("field1 into LocalRegion"), // Good
	@InRegion("field2 into field"),       // Good as of 2009-08-11
	@InRegion("field3 into SuperRegion"), // Good
	@InRegion("field4 into superField")   // Good as of 2009-08-11
})
public class Test extends Super {
  int field;
  
  int field1;
  int field2;
  int field3;
  int field4;
}
