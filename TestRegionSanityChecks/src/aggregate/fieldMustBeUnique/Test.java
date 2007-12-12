package aggregate.fieldMustBeUnique;

import com.surelogic.Aggregate;
import com.surelogic.Unique;

public class Test {
  @Unique
  @Aggregate("Instance into Instance" /* is CONSISTENT */)
  protected C unique;

  @Aggregate("Instance into Instance" /* is UNASSOCIATED */)
  protected C notUnique;

  @Unique
  @Aggregate("Instance into All" /* is CONSISTENT */)
  protected static C uniqueStatic;

  @Aggregate("Instance into All" /* is UNASSOCIATED */)
  protected static C notUniqueStatic;
}
