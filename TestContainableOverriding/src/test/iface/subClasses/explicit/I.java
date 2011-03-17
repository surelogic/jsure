package test.iface.subClasses.explicit;

import com.surelogic.Containable;
import com.surelogic.NotContainable;
import com.surelogic.Unique;

// GOOD
@Containable
public interface I {
  
}

// GOOD
@NotContainable
interface J {
  
}



//BAD: Must be @Containable(implemenationOnly=false)
@NotContainable
class C1 implements I {
  @Unique("return")
  public C1() {}
}

//BAD: Must be @Containable(implemenationOnly=false)
@Containable(implementationOnly=true)
class C2 implements I {
  @Unique("return")
  public C2() {}
}

//GOOD: Must be @Containable(implemenationOnly=false)
@Containable(implementationOnly=false)
class C3 implements I {
  @Unique("return")
  public C3() {}
}

//GOOD: Must be @Containable(implemenationOnly=false)
@Containable
class C4 implements I {
  @Unique("return")
  public C4() {}
}



// GOOD: J has no constraints
@NotContainable
class D1 implements J {
  @Unique("return")
  public D1() {}
}

//GOOD: J has no constraints
@Containable(implementationOnly=true)
class D2 implements J {
  @Unique("return")
  public D2() {}
}

//GOOD: J has no constraints
@Containable(implementationOnly=false)
class D3 implements J {
  @Unique("return")
  public D3() {}
}

//GOOD: J has no constraints
@Containable
class D4 implements J {
  @Unique("return")
  public D4() {}
}



//BAD: Must be @Containable(implemenationOnly=false) (from I)
@NotContainable
class E1 implements I, J {
  @Unique("return")
  public E1() {}
}

//BAD: Must be @Containable(implemenationOnly=false) (from I)
@Containable(implementationOnly=true)
class E2 implements I, J {
  @Unique("return")
  public E2() {}
}

//GOOD: Must be @Containable(implemenationOnly=false) (from I)
@Containable(implementationOnly=false)
class E3 implements I, J {
  @Unique("return")
  public E3() {}
}

//GOOD: Must be @Containable(implemenationOnly=false) (from I)
@Containable
class E4 implements I, J {
  @Unique("return")
  public E4() {}
}


