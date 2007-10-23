package test_parameter;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public static StaticRegion"),
  @Region("public InstanceRegion"),
  @Region("public PublicRegion"),
  @Region("protected ProtectedRegion"),
  @Region("DefaultRegion"),
  @Region("private PrivateRegion")
})
public class C {
}
