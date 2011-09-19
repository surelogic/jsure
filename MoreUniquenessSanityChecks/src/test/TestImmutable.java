package test;

import com.surelogic.Immutable;

@SuppressWarnings("unused")
public class TestImmutable {
	// BAD: Field cannot be primitive
	@Immutable
	private int bad1 = 1;
	
	// GOOD
	@Immutable
	private Object good1;
	
	
	
	
	public void badMethod(@Immutable int x) {}
	
	public void goodMethod(@Immutable Object o) {}
}