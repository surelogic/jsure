package test.iface.subInterfaces.implicit;

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
interface K extends I {
  
}


// Good: not containable
interface X {
  
}

// GOOD: still not containable
interface Y extends X {
  
}

// GOOD: allowed to add Containable
@ThreadSafe
interface Z extends X {
  
}


interface A {
  
}

@ThreadSafe
interface B {
  
}

// BAD: must be containable (from B)
interface C extends A, B {
  
}

// GOOD: must be containable (from B)
@ThreadSafe
interface D extends A, B {
  
}
