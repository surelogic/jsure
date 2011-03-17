package test.clazz.explicit;

import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

// GOOD
@ThreadSafe(implementationOnly=true)
public class C {
  @Unique("return")
  public C() {}
}

// GOOD: Doesn't have to be anything
@NotThreadSafe
class D extends C {
  @Unique("return")
  public D() {}
}

// GOOD: stays containable
@ThreadSafe(implementationOnly=true)
class E extends C {
  @Unique("return")
  public E() {}
}

// BAD: Super is not containable
@ThreadSafe(implementationOnly=true)
class F extends D {
  @Unique("return")
  public F() {}
}



//GOOD: implOnly=false can extend implOnly=true
@ThreadSafe
class W extends C {
  @Unique("return")
  public W() {}
}

//GOOD: implOnly=false can extend implOnly=true
@ThreadSafe(implementationOnly=false)
class WW extends C {
  @Unique("return")
  public WW() {}
}

// Good: implOnly=false can extend implOnly=false
@ThreadSafe
class X extends W {
  @Unique("return")
  public X() {}
}

//Good: implOnly=false can extend implOnly=false
@ThreadSafe(implementationOnly=false)
class XX extends W {
  @Unique("return")
  public XX() {}
}

// BAD: implyOnly=true cannot extend implOnly=false
@ThreadSafe(implementationOnly=true)
class Y extends X {
  @Unique("return")
  public Y() {}
}

//BAD: Super is not containable
@ThreadSafe
class Z extends D {
  @Unique("return")
  public Z() {}
}

//BAD: Super is not containable
@ThreadSafe(implementationOnly=false)
class ZZ extends D {
  @Unique("return")
  public ZZ() {}
}

// BAD: not containable at all
@NotThreadSafe
class Foo extends W {
  @Unique("return")
  public Foo() {}
}

