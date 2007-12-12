package inRegion.staticParents;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("static LocalStatic"),
  @Region("LocalInstance"),
})
public class Test extends Super {
  @InRegion("SuperStatic" /* is CONSISTENT */) // GOOD
  int field1;
  
  @InRegion("SuperInstance" /* is CONSISTENT */) // GOOD
  int field2;
  
  @InRegion("LocalStatic" /* is CONSISTENT */) //GOOD
  int field3;
  
  @InRegion("LocalInstance" /* is CONSISTENT */) // GOOD
  int field4;
  


  @InRegion("SuperStatic" /* is CONSISTENT */) // GOOD
  static int field10;
  
  @InRegion("SuperInstance" /* is UNASSOCIATED */) // BAD
  static int field20;
  
  @InRegion("LocalStatic" /* is CONSISTENT */) //GOOD
  static int field30;
  
  @InRegion("LocalInstance" /* is UNASSOCIATED */) // BAD
  static int field40;
}
