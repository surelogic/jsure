package test;

import com.surelogic.BorrowedInRegion;

@SuppressWarnings("unused")
public class TestExplicitBorrowedInRegion {
	// BAD: Field cannot be primitive
	@BorrowedInRegion("Instance into Instance")
	private final int bad1 = 1;
	
	// BAD: Field must be final
	@BorrowedInRegion("Instance into Instance")
	private Object bad2;
	
	// BAD: Field cannot be static
	@BorrowedInRegion("Instance into Instance")
	private static final Object bad3 = null;
	
	// BAD: Field cannot be static, must be final
	@BorrowedInRegion("Instance into Instance")
	private static Object bad4;
	
	// GOOD: final 
	@BorrowedInRegion("Instance into Instance")
	private final Object good1 = null;
}