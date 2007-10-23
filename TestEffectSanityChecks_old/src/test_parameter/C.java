package test_parameter;

import com.surelogic.Reads;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Writes;

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
