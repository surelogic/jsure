package aggregateInRegion.accessibility.samePackage;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("private PrivateSuper"),
  @Region("DefaultSuper"),
  @Region("protected ProtectedSuper"),
  @Region("public PublicSuper"),
  @Region("private static PrivateStaticSuper"),
  @Region("static DefaultStaticSuper"),
  @Region("protected static ProtectedStaticSuper"),
  @Region("public static PublicStaticSuper")
})
public class Super {
}
