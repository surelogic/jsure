package aggregateInRegion.accessibility.differentPackages.p2;

import aggregateInRegion.accessibility.differentPackages.p1.Super;

import com.surelogic.AggregateInRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@SuppressWarnings("unused")
public class ParentIsSuper extends Super {
  @Unique
	@AggregateInRegion("PrivateSuper" /* is UNASSOCIATED */)
	private final Object toSuperPrivate = new Object();

	@Unique
	@AggregateInRegion("DefaultSuper" /* is UNASSOCIATED */)
	private final Object toSuperDefault = new Object();

	@Unique
	@AggregateInRegion("ProtectedSuper" /* is CONSISTENT */)
	private final Object toSuperProtected = new Object();

	@Unique
	@AggregateInRegion("PublicSuper" /* is CONSISTENT */)
	private final Object toSuperPublic = new Object();



	@Unique
	@AggregateInRegion("PrivateStaticSuper" /* is UNASSOCIATED */)
	private final Object toSuperPrivateStatic = new Object();

	@Unique
	@AggregateInRegion("DefaultStaticSuper" /* is UNASSOCIATED */)
	private final Object toSuperDefaultStatic = new Object();

	@Unique
	@AggregateInRegion("ProtectedStaticSuper" /* is CONSISTENT */)
	private final Object toSuperProtectedStatic = new Object();

	@Unique
	@AggregateInRegion("PublicStaticSuper" /* is CONSISTENT */)
	private final Object toSuperPublicStatic = new Object();
}
