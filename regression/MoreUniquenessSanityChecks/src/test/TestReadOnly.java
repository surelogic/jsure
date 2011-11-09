package test;

import com.surelogic.ReadOnly;

@SuppressWarnings("unused")
public class TestReadOnly {
	// BAD: Field cannot be primitive
	@ReadOnly
	private int bad1 = 1;
	
	// GOOD
	@ReadOnly
	private Object good1;
	
	
	
	
	public void badMethod(@ReadOnly int x) {}
	
	public void goodMethod(@ReadOnly Object o) {}
}