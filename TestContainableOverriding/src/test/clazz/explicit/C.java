package test.clazz.explicit;

import com.surelogic.Containable;
import com.surelogic.NotContainable;
import com.surelogic.Unique;

// GOOD
@Containable(implementationOnly=true)
public class C {
  @Unique("return")
  public C() {}
}

// GOOD: Doesn't have to be anything
@NotContainable
class D extends C {
  @Unique("return")
  public D() {}
}

// GOOD: stays containable
@Containable(implementationOnly=true)
class E extends C {
  @Unique("return")
  public E() {}
}

// BAD: Super is not containable
@Containable(implementationOnly=true)
class F extends D {
  @Unique("return")
  public F() {}
}



//GOOD: implOnly=false can extend implOnly=true
@Containable
class W extends C {
  @Unique("return")
  public W() {}
}

//GOOD: implOnly=false can extend implOnly=true
@Containable(implementationOnly=false)
class WW extends C {
  @Unique("return")
  public WW() {}
}

// Good: implOnly=false can extend implOnly=false
@Containable
class X extends W {
  @Unique("return")
  public X() {}
}

//Good: implOnly=false can extend implOnly=false
@Containable(implementationOnly=false)
class XX extends W {
  @Unique("return")
  public XX() {}
}

// BAD: implyOnly=true cannot extend implOnly=false
@Containable(implementationOnly=true)
class Y extends X {
  @Unique("return")
  public Y() {}
}

//BAD: Super is not containable
@Containable
class Z extends D {
  @Unique("return")
  public Z() {}
}

//BAD: Super is not containable
@Containable(implementationOnly=false)
class ZZ extends D {
  @Unique("return")
  public ZZ() {}
}

// BAD: not containable at all
@NotContainable
class Foo extends W {
  @Unique("return")
  public Foo() {}
}

