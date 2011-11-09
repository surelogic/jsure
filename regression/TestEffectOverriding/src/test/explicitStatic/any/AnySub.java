package test.explicitStatic.any;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public C extends B"),
  
  @Region("public Z")
})
public class AnySub extends Any {

}
