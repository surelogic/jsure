package test.write.receiver.implicit;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

@Regions({
	@Region("public R"),
	@Region("public Q"),
	@Region("public P")
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
	@RegionLock("N is lock3 protects P")
})
public class C {
	public final ReadWriteLock lock1 = new ReentrantReadWriteLock();
	public final ReadWriteLock lock2 = new ReentrantReadWriteLock();
	public final ReadWriteLock lock3 = new ReentrantReadWriteLock();
	
	
	
	@RequiresLock("L.writeLock(), M.writeLock()")
	public void m1(final C a, final C b, final C c) {}
	
	@RequiresLock("L.writeLock()")
	public void m2(final C a, final C b, final C c) {}
	
	// Explicit empty
	@RequiresLock("")
	public void m3(final C a) {}
	
	// Implicit empty
	public void m4(final C a) {}	
}



/* BASIC CHECKS:
 * (1) Can completely remove requirements
 * (2) @RequiresLock("") is equivalent to no @RequiresLock
 */
class Good1 extends C {
	// GOOD: Can remove requirements; use implicit empty
	@Override
	public void m1(final C a, final C b, final C c) {}
	
	// GOOD: Can remove requirements: use explicit empty
	@Override
	@RequiresLock("")
	public void m2(final C a, final C b, final C c) {}
	
	// GOOD: Same as explicit empty
	@Override
	public void m3(final C a) {}
	
	// GOOD: Same as implicit empty
	@Override
	@RequiresLock("")
	public void m4(final C a) {}
}



/* BASIC CHECKS: Cannot add requirements
 */
class Bad1 extends C {
	// BAD: Cannot add lock
	@Override
	@RequiresLock("L.writeLock(), M.writeLock(), N.writeLock()")
	public void m1(final C a, final C b, final C c) {}
	
	// BAD: Cannot add lock to explicit empty
	@Override
	@RequiresLock("L.writeLock()")
	public void m3(final C a) {}
	
	// BAD: Cannot add lock to implicit empty
	@Override
	@RequiresLock("M.writeLock()")
	public void m4(final C a) {}		
}



/* Can partially remove requirements */
class Good2a extends C {
	// GOOD: Can remove requirements;
	@Override
	@RequiresLock("L.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Good2b extends C {
	// GOOD: Can remove requirements;
	@Override
	@RequiresLock("L.readLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Good3a extends C {
	// GOOD: Can remove requirements
	@Override
	@RequiresLock("M.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Good3b extends C {
	// GOOD: Can remove requirements
	@Override
	@RequiresLock("M.readLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Bad2 extends C {
	// BAD: Completely different
	@Override
	@RequiresLock("N.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}


// === Test against other ways of naming locks ===

/*
 * (1) parameter renaming (correct and wrong)
 * (2) implicit receiver
 * (3) explicit receiver
 * (4) 0th-qualified receiver
 * (6) static locks
 *
 * Also, correct name, but wrong lock
 * 
 * The above, cross read lock and write lock
 */

@Regions({
	@Region("public R"),
	@Region("public Q"),
	@Region("public I"),
	@Region("public static S"),
	@Region("public static S_I")
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
	@RegionLock("LL is this protects I"),
	@RegionLock("N is staticRWLock protects S"),
	@RegionLock("NN is class protects S_I")
})
class X {
	public final ReadWriteLock lock1 = new ReentrantReadWriteLock();
	public final ReadWriteLock lock2 = new ReentrantReadWriteLock();
	public final static ReadWriteLock staticRWLock = new ReentrantReadWriteLock();
	
	@RequiresLock("L.writeLock()")
	public void m(final X a, final X b) {}
}

// ==== Intrinsic Locks ====

// FORMALS

class XBad1 extends X {
	// BAD: formal argument, wrong lock
	@Override
	@RequiresLock("a:LL")
	public void m(final X a, final X b) {}
}

class XBad2 extends X {
	// BAD: formal argument (renamed), wrong lock
	@Override
	@RequiresLock("aa:LL")
	public void m(final X aa, final X bb) {}
}

// RECEIVERS

class XBad3 extends X {
	// BAD: implicit receiver, wrong lock
	@Override
	@RequiresLock("LL")
	public void m(final X a, final X b) {}
}

class XBad4 extends X {
	// BAD: explicit receiver, wrong lock
	@Override
	@RequiresLock("this:LL")
	public void m(final X a, final X b) {}
}

class XBad5 extends X {
	// BAD: 0th-qualified receiver, wrong lock
	@Override
	@RequiresLock("XBad5.this:LL")
	public void m(final X a, final X b) {}
}

// STATIC LOCK

class XBad6 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("NN")
	public void m(final X a, final X b) {}
}

class XBad7 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.write.receiver.implicit.X:NN")
	public void m(final X a, final X b) {}
}

class XBad8 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.write.receiver.implicit.XBad8:NN")
	public void m(final X a, final X b) {}
}

// ==== Read Locks ====

// FORMALS

class XBad9 extends X {
	// BAD: formal argument, correct lock
	@Override
	@RequiresLock("a:L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad10 extends X {
	// BAD: formal argument (renamed), correct lock
	@Override
	@RequiresLock("aa:L.readLock()")
	public void m(final X aa, final X bb) {
	}
}

// RECEIVERS

class XGood1 extends X {
	// GOOD: implicit receiver, can decrease locking requirement
	@Override
	@RequiresLock("L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XGood2 extends X {
	// GOOD: explicit receiver, can decrease locking requirement
	@Override
	@RequiresLock("this:L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XGood3 extends X {
	// GOOD: 0th-qualified receiver, can decrease locking requirement
	@Override
	@RequiresLock("XGood3.this:L.readLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XBad11 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("N.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad12 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.write.receiver.implicit.X:N.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad13 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.write.receiver.implicit.XBad13:N.readLock()")
	public void m(final X a, final X b) {
	}
}

// ==== Write Locks ====

// FORMALS

class XBad14 extends X {
	// BAD: formal argument, correct lock
	@Override
	@RequiresLock("a:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad15 extends X {
	// BAD: formal argument (renamed), correct lock
	@Override
	@RequiresLock("aa:L.writeLock()")
	public void m(final X aa, final X bb) {
	}
}

// RECEIVERS

class XGood4 extends X {
	// GOOD: implicit receiver
	@Override
	@RequiresLock("L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XGood5 extends X {
	// GOOD: explicit receiver
	@Override
	@RequiresLock("this:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XGood6 extends X {
	// GOOD: 0th-qualified receiver
	@Override
	@RequiresLock("XGood6.this:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XBad16 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("N.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad17 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.write.receiver.implicit.X:N.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad18 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.write.receiver.implicit.XBad18:N.writeLock()")
	public void m(final X a, final X b) {
	}
}
