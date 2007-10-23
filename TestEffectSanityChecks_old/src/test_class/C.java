package test_class;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public static StaticRegion"),
  @Region("public InstanceRegion"),
  @Region("public static PublicRegion"),
  @Region("protected static ProtectedRegion"),
  @Region("static DefaultRegion"),
  @Region("private static PrivateRegion")
})
public class C {

}
