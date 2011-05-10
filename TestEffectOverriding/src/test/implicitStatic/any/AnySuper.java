package test.implicitStatic.any;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public static T"),
  @Region("public A extends T"),
  
  @Region("public static O"),
  @Region("public X extends O")
})
public class AnySuper {

}
