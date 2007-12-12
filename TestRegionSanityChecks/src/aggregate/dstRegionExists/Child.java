package aggregate.dstRegionExists;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public Child"),
  @Region("public static ChildStatic")
})
public class Child extends Test {
  // do nothing
}
