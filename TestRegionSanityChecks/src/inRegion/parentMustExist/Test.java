package inRegion.parentMustExist;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("LocalRegion"),
})
public class Test extends Super {
  @SuppressWarnings("unused")
  @InRegion("NoSuchRegion" /* is UNBOUND */) // BAD: parent doesn't exist
  private int field1;
  
  @SuppressWarnings("unused")
  @InRegion("Instance" /* is CONSISTENT */) // GOOD: Instance exists (always)
  private int field2;
  
  @SuppressWarnings("unused")
  @InRegion("SuperRegion" /* is CONSISTENT */) // GOOD: Region from parent
  private int field3;
  
  @SuppressWarnings("unused")
  @InRegion("ChildRegion" /* is UNBOUND */) // BAD: Region from child
  private int field4;
  
  @SuppressWarnings("unused")
  @InRegion("UnrelatedRegion" /* is UNBOUND */) // BAD: Region from unrelated class
  private int field5;
  
  @SuppressWarnings("unused")
  @InRegion("LocalRegion" /* is CONSISTENT */) // Good: Region from same class
  private int field6;
}
