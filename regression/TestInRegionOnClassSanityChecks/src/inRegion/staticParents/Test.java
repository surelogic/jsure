package inRegion.staticParents;

import com.surelogic.InRegion;
import com.surelogic.InRegions;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("static LocalStatic"),
  @Region("LocalInstance"),
})
@InRegions({
	@InRegion("field1 into SuperStatic" /* is CONSISTENT */),
	@InRegion("field2 into SuperInstance" /* is CONSISTENT */),
	@InRegion("field3 into LocalStatic" /* is CONSISTENT */),
	@InRegion("field4 into LocalInstance" /* is CONSISTENT */),

	@InRegion("field10 into SuperStatic" /* is CONSISTENT */),
	@InRegion("field20 into SuperInstance" /* is UNASSOCIATED */),
	@InRegion("field30 into LocalStatic" /* is CONSISTENT */),
	@InRegion("field40 into LocalInstance" /* is UNASSOCIATED */),
})
public class Test extends Super {
  int field1;
  int field2;
  int field3;
  int field4;
  
  static int field10;
  static int field20;
  static int field30;
  static int field40;
}
