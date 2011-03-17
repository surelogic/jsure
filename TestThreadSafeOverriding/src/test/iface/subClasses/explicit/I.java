package test.iface.subClasses.explicit;

import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

// GOOD
@ThreadSafe
public interface I {
  
}

// GOOD
@NotThreadSafe
interface J {
  
}



//BAD: Must be @ThreadSafe(implemenationOnly=false)
@NotThreadSafe
class C1 implements I {
  @Unique("return")
  public C1() {}
}

//BAD: Must be @ThreadSafe(implemenationOnly=false)
@ThreadSafe(implementationOnly=true)
class C2 implements I {
  @Unique("return")
  public C2() {}
}

//GOOD: Must be @ThreadSafe(implemenationOnly=false)
@ThreadSafe(implementationOnly=false)
class C3 implements I {
  @Unique("return")
  public C3() {}
}

//GOOD: Must be @ThreadSafe(implemenationOnly=false)
@ThreadSafe
class C4 implements I {
  @Unique("return")
  public C4() {}
}



// GOOD: J has no constraints
@NotThreadSafe
class D1 implements J {
  @Unique("return")
  public D1() {}
}

//GOOD: J has no constraints
@ThreadSafe(implementationOnly=true)
class D2 implements J {
  @Unique("return")
  public D2() {}
}

//GOOD: J has no constraints
@ThreadSafe(implementationOnly=false)
class D3 implements J {
  @Unique("return")
  public D3() {}
}

//GOOD: J has no constraints
@ThreadSafe
class D4 implements J {
  @Unique("return")
  public D4() {}
}



//BAD: Must be @ThreadSafe(implemenationOnly=false) (from I)
@NotThreadSafe
class E1 implements I, J {
  @Unique("return")
  public E1() {}
}

//BAD: Must be @ThreadSafe(implemenationOnly=false) (from I)
@ThreadSafe(implementationOnly=true)
class E2 implements I, J {
  @Unique("return")
  public E2() {}
}

//GOOD: Must be @ThreadSafe(implemenationOnly=false) (from I)
@ThreadSafe(implementationOnly=false)
class E3 implements I, J {
  @Unique("return")
  public E3() {}
}

//GOOD: Must be @ThreadSafe(implemenationOnly=false) (from I)
@ThreadSafe
class E4 implements I, J {
  @Unique("return")
  public E4() {}
}


