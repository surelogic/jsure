package testFieldDeclarations;

import com.surelogic.Containable;
import com.surelogic.Unique;

@Containable
public class ContainableType {
	@Unique("return")
	public ContainableType() {}
}
