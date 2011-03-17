package test.iface.subInterfaces.implicit;

import com.surelogic.Containable;

// GOOD
@Containable
public interface I {

}

// GOOD: Must be containable
@Containable
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
@Containable
interface Z extends X {
  
}


interface A {
  
}

@Containable
interface B {
  
}

// BAD: must be containable (from B)
interface C extends A, B {
  
}

// GOOD: must be containable (from B)
@Containable
interface D extends A, B {
  
}
