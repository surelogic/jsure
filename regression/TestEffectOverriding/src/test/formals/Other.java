package test.formals;

import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public Other"),
  @Region("public B") // same name
})
public class Other {

}
