package test.simple.nonFinal;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("public R"),
  @Region("public Q"),
  
  @Region("public static S"),
  @Region("public static T")
})
@SuppressWarnings("unused")
public class Test {
  @UniqueInRegion("R")
  private Object f1 = new Object();
  
  @UniqueInRegion("R")
  @InRegion("Q")
  private Object f2 = new Object();

  @UniqueInRegion("S")
  private static Object f4 = new Object();
  
  @UniqueInRegion("S")
  @InRegion("T")
  private static Object f3 = new Object();
}
