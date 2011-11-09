package test.iface.subInterfaces.implicit;

import com.surelogic.Immutable;

// GOOD
@Immutable
public interface I {

}

// GOOD: Must be containable
@Immutable
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
@Immutable
interface Z extends X {
  
}


interface A {
  
}

@Immutable
interface B {
  
}

// BAD: must be containable (from B)
interface C extends A, B {
  
}

// GOOD: must be containable (from B)
@Immutable
interface D extends A, B {
  
}
