package test.iface.subInterfaces.explicit;

import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;

// GOOD
@ThreadSafe
public interface I {

}

// GOOD: Must be containable
@ThreadSafe
interface J extends I {
  
}

// BAD: Must be containable
@NotThreadSafe
interface K extends I {
  
}


// Good: not containable
@NotThreadSafe
interface X {
  
}

// GOOD: still not containable
@NotThreadSafe
interface Y extends X {
  
}

// GOOD: allowed to add Containable
@ThreadSafe
interface Z extends X {
  
}


@NotThreadSafe
interface A {
  
}

@ThreadSafe
interface B {
  
}

// BAD: must be containable (from B)
@NotThreadSafe
interface C extends A, B {
  
}

// GOOD: must be containable (from B)
@ThreadSafe
interface D extends A, B {
  
}
