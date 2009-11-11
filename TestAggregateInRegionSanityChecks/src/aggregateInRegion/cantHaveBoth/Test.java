package aggregateInRegion.cantHaveBoth;

import com.surelogic.Aggregate;
import com.surelogic.AggregateInRegion;
import com.surelogic.Unique;

public class Test {
	@Unique
	@Aggregate(/* is CONSISTENT */) // also test that no-arg @Aggregate works
	private final Object f1 = new Object();

	@Unique
	@AggregateInRegion("Instance" /* is CONSISTENT */)
	private final Object f2 = new Object();

	@Unique
	@Aggregate(/* is UNASSOCIATED */)
	@AggregateInRegion("Instance" /* is CONSISTENT */)
	private final Object f3 = new Object();
}
