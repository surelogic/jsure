package test.qualifiedThis.any;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public E extends B"),
  
  @Region("public Z")
})
public class AnySub extends Any {

}
