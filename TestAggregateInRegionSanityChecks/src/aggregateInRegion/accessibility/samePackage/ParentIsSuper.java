package aggregateInRegion.accessibility.samePackage;

import com.surelogic.AggregateInRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("private PrivateLocal"),
  @Region("DefaultLocal"),
  @Region("protected ProtectedLocal"),
  @Region("public PublicLocal"),
  @Region("private static PrivateStaticLocal"),
  @Region("static DefaultStaticLocal"),
  @Region("protected static ProtectedStaticLocal"),
  @Region("public static PublicStaticLocal")
})
@SuppressWarnings("unused")
public class ParentIsSuper extends Super {
  @Unique
	@AggregateInRegion("PrivateSuper" /* is UNASSOCIATED */)
	private final Object toSuperPrivate = new Object();

	@Unique
	@AggregateInRegion("DefaultSuper" /* is CONSISTENT */)
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
	@AggregateInRegion("DefaultStaticSuper" /* is CONSISTENT */)
	private final Object toSuperDefaultStatic = new Object();

	@Unique
	@AggregateInRegion("ProtectedStaticSuper" /* is CONSISTENT */)
	private final Object toSuperProtectedStatic = new Object();

	@Unique
	@AggregateInRegion("PublicStaticSuper" /* is CONSISTENT */)
	private final Object toSuperPublicStatic = new Object();



	@Unique
	@AggregateInRegion("PrivateLocal" /* is CONSISTENT */)
	private final Object toLocalPrivate = new Object();

	@Unique
	@AggregateInRegion("DefaultLocal" /* is CONSISTENT */)
	private final Object toLocalDefault = new Object();

	@Unique
	@AggregateInRegion("ProtectedLocal" /* is CONSISTENT */)
	private final Object toLocalProtected = new Object();

	@Unique
	@AggregateInRegion("PublicLocal" /* is CONSISTENT */)
	private final Object toLocalPublic = new Object();



	@Unique
	@AggregateInRegion("PrivateStaticLocal" /* is CONSISTENT */)
	private final Object toLocalPrivateStatic = new Object();

	@Unique
	@AggregateInRegion("DefaultStaticLocal" /* is CONSISTENT */)
	private final Object toLocalDefaultStatic = new Object();

	@Unique
	@AggregateInRegion("ProtectedStaticLocal" /* is CONSISTENT */)
	private final Object toLocalProtectedStatic = new Object();

	@Unique
	@AggregateInRegion("PublicStaticLocal" /* is CONSISTENT */)
	private final Object toLocalPublicStatic = new Object();
}
