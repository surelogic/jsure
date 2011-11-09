package test.fancy.accessibility.samePackage;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("DefaultAgg")
public class DefaultDelegate {
	@Borrowed("this")
	public DefaultDelegate() {
		super();
	}
}
