package test;

import com.surelogic.Borrowed;

@SuppressWarnings("unused")
public class TestBorrowed {
	// BAD: Field cannot be primitive
	@Borrowed
	private final int bad1 = 1;
	
	// BAD: Field must be final
	@Borrowed
	private Object bad2;
	
	// BAD: Field cannot be static
	@Borrowed
	private static final Object bad3 = null;
	
	// BAD: Field cannot be static, must be final
	@Borrowed
	private static Object bad4;
	
	// GOOD: final 
	@Borrowed
	private final Object good1 = null;
	
	
	
	public void badMethod(@Borrowed int x) {}
	
	public void goodMethod(@Borrowed Object o) {}
}