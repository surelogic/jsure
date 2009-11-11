package aggregateInRegion.fieldMustBeUnique;

import com.surelogic.AggregateInRegion;
import com.surelogic.Unique;

public class Test {
  @Unique
  @AggregateInRegion("Instance" /* is CONSISTENT */)
  protected C unique;

  @AggregateInRegion("Instance" /* is UNASSOCIATED */)
  protected C notUnique;

  @Unique
  @AggregateInRegion("All" /* is CONSISTENT */)
  protected static C uniqueStatic;

  @AggregateInRegion("All" /* is UNASSOCIATED */)
  protected static C notUniqueStatic;
}
