package test.read.formals;

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
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
})
public class C {
	public final ReadWriteLock lock1 = new ReentrantReadWriteLock();
	public final ReadWriteLock lock2 = new ReentrantReadWriteLock();
	
	
	@RequiresLock("a:L.readLock(), b:M.readLock()")
	public void m1(final C a, final C b, final C c) {}
	
	@RequiresLock("a:L.readLock()")
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
	@RequiresLock("a:L.readLock(), b:M.readLock(), c:L.readLock()")
	public void m1(final C a, final C b, final C c) {}
	
	// BAD: Cannot add lock to explicit empty
	@Override
	@RequiresLock("a:L.readLock()")
	public void m3(final C a) {}
	
	// BAD: Cannot add lock to implicit empty
	@Override
	@RequiresLock("a:M.readLock()")
	public void m4(final C a) {}		
}



/* Can partially remove requirements */
class Good2a extends C {
	// GOOD: Can remove requirements;
	@Override
	@RequiresLock("a:L.readLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Bad2b extends C {
	// BAD: Can remove requirements;  CANNOT UPGRADE TO WRITE LOCK
	@Override
	@RequiresLock("a:L.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Good3a extends C {
	// GOOD: Can remove requirements
	@Override
	@RequiresLock("b:M.readLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Bad3b extends C {
	// BAD: Can remove requirements  CANNOT UPGRADE TO WRITE LOCK
	@Override
	@RequiresLock("b:M.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Bad2 extends C {
	// BAD: Completely different
	@Override
	@RequiresLock("c:M.readLock()")
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
	@Region("public RW"),
	@Region("public static S"),
	@Region("public static S_RW")
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
	@RegionLock("LL is this protects RW"),
	@RegionLock("N is class protects S"),
	@RegionLock("NN is staticRWLock protects S_RW")
})
class X {
	public final ReadWriteLock lock1 = new ReentrantReadWriteLock();
	public final ReadWriteLock lock2 = new ReentrantReadWriteLock();
	public final static ReadWriteLock staticRWLock = new ReentrantReadWriteLock();
	
	@RequiresLock("a:L.readLock()")
	public void m(final X a, final X b) {}
}

// ==== Intrinsic Locks ====

// FORMALS

class XBad1 extends X {
	// BAD: formal identical, wrong lock
	@Override
	@RequiresLock("a:LL")
	public void m(final X a, final X b) {}
}

class XBad2 extends X {
	// BAD: formal renamed, wrong lock
	@Override
	@RequiresLock("aa:LL")
	public void m(final X aa, final X bb) {}
}

// RECEIVERS

class XBad3 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("LL")
	public void m(final X a, final X b) {}
}

class XBad4 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:LL")
	public void m(final X a, final X b) {}
}

class XBad5 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad5.this:LL")
	public void m(final X a, final X b) {}
}

// STATIC LOCK

class XBad6 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("N")
	public void m(final X a, final X b) {}
}

class XBad7 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.read.formals.X:N")
	public void m(final X a, final X b) {}
}

class XBad8 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.read.formals.XBad8:N")
	public void m(final X a, final X b) {}
}

//==== Read Locks ====

// FORMALS

class XGood1 extends X {
	// GOOD: formal identical, read lock
	@Override
	@RequiresLock("a:L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XGood2 extends X {
	// GOOD: formal renamed, read lock
	@Override
	@RequiresLock("aa:L.readLock()")
	public void m(final X aa, final X bb) {
	}
}

class XBad9 extends X {
	// BAD: wrong formal, read lock
	@Override
	@RequiresLock("b:L.readLock()")
	public void m(final X a, final X b) {
	}
}

// RECEIVERS

class XBad10 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad11 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad12 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad12.this:L.readLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XBad13 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("NN.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad14 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.read.formals.X:NN.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad15 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.read.formals.XBad15:NN.readLock()")
	public void m(final X a, final X b) {
	}
}

// ==== Write Locks ====

// FORMALS

class XBad16 extends X {
	// BAD: formal identical, write lock (Cannot increase requirements)
	@Override
	@RequiresLock("a:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad17 extends X {
	// BAD: formal identical, wrong lock, write lock
	@Override
	@RequiresLock("a:M.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad18 extends X {
	// BAD: formal renamed, write lock (Cannot increase requirements)
	@Override
	@RequiresLock("aa:L.writeLock()")
	public void m(final X aa, final X bb) {
	}
}

class XBad19 extends X {
	// BAD: formal renamed, wrong lock, write lock
	@Override
	@RequiresLock("aa:M.writeLock()")
	public void m(final X aa, final X bb) {
	}
}

class XBad20 extends X {
	// BAD: wrong formal, read lock
	@Override
	@RequiresLock("b:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

// RECEIVERS

class XBad21 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad22 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad23 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad23.this:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XBad24 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("NN.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad25 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.read.formals.X:NN.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad26 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.read.formals.XBad26:NN.writeLock()")
	public void m(final X a, final X b) {
	}
}
