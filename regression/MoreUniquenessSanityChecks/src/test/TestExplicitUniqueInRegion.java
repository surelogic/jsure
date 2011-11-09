package test;

import com.surelogic.UniqueInRegion;

@SuppressWarnings("unused")
public class TestExplicitUniqueInRegion {
	// BAD: Field cannot be primitive
	@UniqueInRegion("Instance into Instance")
	private final int bad1 = 1;
}