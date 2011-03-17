package test.iface.subInterfaces.explicit;

import com.surelogic.Immutable;
import com.surelogic.Mutable;

// GOOD
@Immutable
public interface I {

}

// GOOD: Must be containable
@Immutable
interface J extends I {
  
}

// BAD: Must be containable
@Mutable
interface K extends I {
  
}


// Good: not containable
@Mutable
interface X {
  
}

// GOOD: still not containable
@Mutable
interface Y extends X {
  
}

// GOOD: allowed to add Containable
@Immutable
interface Z extends X {
  
}


@Mutable
interface A {
  
}

@Immutable
interface B {
  
}

// BAD: must be containable (from B)
@Mutable
interface C extends A, B {
  
}

// GOOD: must be containable (from B)
@Immutable
interface D extends A, B {
  
}
