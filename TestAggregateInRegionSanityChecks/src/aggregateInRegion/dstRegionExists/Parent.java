package aggregateInRegion.dstRegionExists;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public Parent"),
  @Region("public static ParentStatic")
})
public class Parent {
  // do nothing
}
