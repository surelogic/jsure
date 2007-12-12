package aggregate.fieldIsObject;

import com.surelogic.Aggregate;
import com.surelogic.Unique;

public class Test {
  @Unique
  @Aggregate("Instance into Instance" /* is UNBOUND */)
  protected int field;
}

