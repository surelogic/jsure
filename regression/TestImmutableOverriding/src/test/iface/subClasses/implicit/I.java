package test.iface.subClasses.implicit;

import com.surelogic.Immutable;
import com.surelogic.Unique;

// GOOD
@Immutable
public interface I {
  
}

// GOOD
interface J {
  
}



//BAD: Must be @Immutable(implemenationOnly=false)
class C1 implements I {
  @Unique("return")
  public C1() {}
}

//BAD: Must be @Immutable(implemenationOnly=false)
@Immutable(implementationOnly=true)
class C2 implements I {
  @Unique("return")
  public C2() {}
}

//GOOD: Must be @Immutable(implemenationOnly=false)
@Immutable(implementationOnly=false)
class C3 implements I {
  @Unique("return")
  public C3() {}
}

//GOOD: Must be @Immutable(implemenationOnly=false)
@Immutable
class C4 implements I {
  @Unique("return")
  public C4() {}
}



// GOOD: J has no constraints
class D1 implements J {
  @Unique("return")
  public D1() {}
}

//GOOD: J has no constraints
@Immutable(implementationOnly=true)
class D2 implements J {
  @Unique("return")
  public D2() {}
}

//GOOD: J has no constraints
@Immutable(implementationOnly=false)
class D3 implements J {
  @Unique("return")
  public D3() {}
}

//GOOD: J has no constraints
@Immutable
class D4 implements J {
  @Unique("return")
  public D4() {}
}



//BAD: Must be @Immutable(implemenationOnly=false) (from I)
class E1 implements I, J {
  @Unique("return")
  public E1() {}
}

//BAD: Must be @Immutable(implemenationOnly=false) (from I)
@Immutable(implementationOnly=true)
class E2 implements I, J {
  @Unique("return")
  public E2() {}
}

//GOOD: Must be @Immutable(implemenationOnly=false) (from I)
@Immutable(implementationOnly=false)
class E3 implements I, J {
  @Unique("return")
  public E3() {}
}

//GOOD: Must be @Immutable(implemenationOnly=false) (from I)
@Immutable
class E4 implements I, J {
  @Unique("return")
  public E4() {}
}


