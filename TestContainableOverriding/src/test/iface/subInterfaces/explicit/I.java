package test.iface.subInterfaces.explicit;

import com.surelogic.Containable;
import com.surelogic.NotContainable;

// GOOD
@Containable
public interface I {

}

// GOOD: Must be containable
@Containable
interface J extends I {
  
}

// BAD: Must be containable
@NotContainable
interface K extends I {
  
}


// Good: not containable
@NotContainable
interface X {
  
}

// GOOD: still not containable
@NotContainable
interface Y extends X {
  
}

// GOOD: allowed to add Containable
@Containable
interface Z extends X {
  
}


@NotContainable
interface A {
  
}

@Containable
interface B {
  
}

// BAD: must be containable (from B)
@NotContainable
interface C extends A, B {
  
}

// GOOD: must be containable (from B)
@Containable
interface D extends A, B {
  
}
