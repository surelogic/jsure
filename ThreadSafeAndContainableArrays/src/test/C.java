package test;

import com.surelogic.Containable;
import com.surelogic.Unique;

@Containable
public class C {
	@Unique("return")
	public C() {
		super();
	}
	
	// GOOD: Unique and containable
	@Unique
	private final int[] good1 = new int[1];

	// BAD: NOT Unique, but containable
	private final int[] bad1a = new int[1];

	// BAD: unique but not containable
	@Unique
	private final Object[] bad1b = new Object[1];

	// GOOD: Unique and containable
	@Unique
	private final byte[] good2 = new byte[1];

	// BAD: NOT unique, but containable
	private final byte[] bad2a = new byte[1];

	// bad: unique, but not containable
	@Unique 
	private final int[][] bad2b = new int[1][];
}
