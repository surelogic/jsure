package test_constructorVisibility;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public PublicRegion"),
  @Region("protected ProtectedRegion"),
  @Region("DefaultRegion"),
  @Region("private static PrivateRegion")
})
public class C {

}
