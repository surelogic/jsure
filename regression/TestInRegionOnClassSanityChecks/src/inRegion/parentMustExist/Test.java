package inRegion.parentMustExist;

import com.surelogic.InRegion;
import com.surelogic.InRegions;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
})
@InRegions({
	@InRegion("field1 into NoSuchRegion" /* is UNBOUND */), // BAD: parent does not exist
  @InRegion("field2 into Instance" /* is CONSISTENT */), // GOOD: Instance exists (always)
  @InRegion("field3 into SuperRegion" /* is CONSISTENT */), // GOOD: Region from parent
  @InRegion("field4 into ChildRegion" /* is UNBOUND */), // BAD: Region from child
  @InRegion("field5 into UnrelatedRegion" /* is UNBOUND */), // BAD: Region from unrelated class
  @InRegion("field6 into LocalRegion" /* is CONSISTENT */) // GOOD: Region from same class
})
@SuppressWarnings("unused")
public class Test extends Super {
  private int field1;
  private int field2;
  private int field3;
  private int field4;
  private int field5;
  private int field6;
}
