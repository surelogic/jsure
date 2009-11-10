package aggregate.accessibility.samePackage;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("public PublicAgg")
public class PublicDelegate {
	@Borrowed("this")
	public PublicDelegate() {
		super();
	}
}
