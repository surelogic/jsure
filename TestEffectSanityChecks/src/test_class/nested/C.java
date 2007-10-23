package test_class.nested;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public static StaticRegion"),
  @Region("public InstanceRegion")
})
public class C {

}
