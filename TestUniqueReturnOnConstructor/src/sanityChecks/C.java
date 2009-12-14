package sanityChecks;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class C {
	/* Traditional case */
	@Unique("return" /* is CONSISTENT */)
	public Object uniqueReturn() {
		return new Object();
	}
	
	/* Traditional case */
	@Unique("this" /* is CONSISTENT */)
	public Object uniqueThis() {
		return new Object();
	}
	
	/* Traditional case */
	@Unique("return, this" /* is CONSISTENT */)
	public Object uniqueReturnThis() {
		return new Object();
	}
	
	/* Traditional case */
	@Unique("this, return" /* is CONSISTENT */)
	public Object uniqueThisReturn() {
		return new Object();
	}
	
	/* Bad: void method */
	@Unique("return" /* is UNASSOCIATED */)
	public void voidMethod_uniqueReturn() {
		// do nothing
	}

	
	/* GOOD: void method */
	@Unique("this" /* is CONSISTENT */)
	public void voidMethod_uniqueThis() {
		// do nothing
	}

	
	/* Bad: scalar method */
	@Unique("return" /* is UNASSOCIATED */)
	public int intMethod() {
		return 0;
	}
	
	/* NEW: on constructor */
	@Unique("return" /* is CONSISTENT */)
	public C() {
		super();
	}
	
	/* NEW BAD: cannot use "this" on constructor */
	@Unique("this" /* is UNPARSEABLE */)
	public C(final int x) {
		super();
	}
	
	/* NEW BAD: cannot use "this" on constructor */
	@Unique("this, return" /* is UNPARSEABLE */)
	public C(final int x, final int y) {
		super();
	}
	
	/* NEW BAD: cannot use "this" on constructor */
	@Unique("return, this" /* is UNPARSEABLE */)
	public C(final int x, final int y, final int z) {
		super();
	}
	
	
	/* STILL GOOD: Old way of doing things */
	@Borrowed("this" /* is CONSISTENT */)
	public C(final double x) {
		super();
	}
	
	/* BAD: Don't mix old and new --- which should flag the error? */
	@Borrowed("this" /* is UNASSOCIATED */)
	@Unique("return" /* is CONSISTENT */)
	public C(final double x, final double y) {
		super();
	}

	/* OK: Can mix the two on regular methods */
	@Borrowed("this" /* is CONSISTENT */)
	@Unique("return" /* is CONSISTENT */)
	public Object foo() {
		return null;
	}
}
