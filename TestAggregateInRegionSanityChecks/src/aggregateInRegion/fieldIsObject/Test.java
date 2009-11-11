package aggregateInRegion.fieldIsObject;

import com.surelogic.AggregateInRegion;
import com.surelogic.Unique;

public class Test {
  @Unique
  @AggregateInRegion("Instance" /* is UNBOUND */)
  protected int field;
}

