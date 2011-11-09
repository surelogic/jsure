package test.fancy.accessibility.samePackage;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("protected ProtectedAgg")
public class ProtectedDelegate {
	@Borrowed("this")
	public ProtectedDelegate() {
		super();
	}
}
