package test;

import com.surelogic.RegionLock;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

@ThreadSafe
@RegionLock("L is this protects Instance")
public class TS {
	// GOOD: final, aggregated containable object
	@Unique
	private final int[] good1 = new int[1];

	// BAD: final and containable, but not aggregated
	private final int[] bad11 = new int[1];

	// BAD: final and aggregated, but not containable
	@Unique
	private final Object[] bad1 = new Object[1];
	
	// GOOD: final, aggregated containable object
	@Unique
	private final byte[] good2 = new byte[1];
	
	// BAD: final and containable, but not aggregated
	private final byte[] bad22 = new byte[1];
	
	// BAD: final and aggregated, but not containable
	@Unique 
	private final int[][] bad2 = new int[1][];
}
