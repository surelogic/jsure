package test;

import com.surelogic.UniqueInRegion;

@SuppressWarnings("unused")
public class TestSimpleUniqueInRegion {
	// BAD: Field cannot be primitive
	@UniqueInRegion("Instance")
	private final int bad1 = 1;
}