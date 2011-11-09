package test.qualifiedThis.any;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public A"),
  @Region("public B extends A"),
  @Region("public C extends B"),
  
  @Region("public X")
})
public class AnySuper {

}
