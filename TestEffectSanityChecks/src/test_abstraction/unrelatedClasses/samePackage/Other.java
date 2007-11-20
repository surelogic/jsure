package test_abstraction.unrelatedClasses.samePackage;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public Public"),
  @Region("protected Protected"),
  @Region("Default"),
  @Region("private Private")
})
public class Other {
}
