package test;

import com.surelogic.Starts;

public interface I {
  // No annotation: starts anything
  public void m();
}

interface J {
  @Starts("nothing")
  public void m();
}

interface II {
  // No annotation: starts anything
  public void m();
}

interface JJ {
  @Starts("nothing")
  public void m();
}

// =============

interface I1 extends I {
  // GOOD
  // No annotation: starts anything
  public void m();
}

interface I2 extends I {
  // GOOD
  @Starts("nothing")
  public void m();
}

interface I3 extends I, II {
  // GOOD
  // No annotation: starts anything
  public void m();
}

interface I4 extends I, II {
  // GOOD
  @Starts("nothing")
  public void m();
}

// =============

interface J1 extends J {
  // BAD
  // No annotation: starts anything
  public void m();
}

interface J2 extends J {
  // GOOD
  @Starts("nothing")
  public void m();
}

interface J3 extends I, J, II, JJ {
  // BAD: J and JJ
  // No annotation: starts anything
  public void m();
}

interface J4 extends I, J, II, JJ {
  // GOOD
  @Starts("nothing")
  public void m();
}

// =============

class A {
  // No annotation: starts anything
  public void m() {
  }
}

class B {
  @Starts("nothing")
  public void m() {
  }
}

// =============

class A1 extends A {
  // GOOD
  // No annotation: starts anything
  public void m() {
  }
}

class A2 extends A {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

class B1 extends B {
  // BAD
  // No annotation: starts anything
  public void m() {
  }
}

class B2 extends B {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

// ================

class A3 extends A implements I {
  // GOOD
  // No annotation: starts anything
  public void m() {
  }
}

class A4 extends A implements I {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

class B3 extends B implements I {
  // BAD
  // No annotation: starts anything
  public void m() {
  }
}

class B4 extends B implements I {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

// ================

class A5 extends A implements J {
  // BAD: J
  // No annotation: starts anything
  public void m() {
  }
}

class A6 extends A implements J {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

class B5 extends B implements J {
  // BAD: B and J
  // No annotation: starts anything
  public void m() {
  }
}

class B6 extends B implements J {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

// ================

class A7 extends A implements I, J, II, JJ {
  // BAD: J and JJ
  // No annotation: starts anything
  public void m() {
  }
}

class A8 extends A implements I, J, II, JJ {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}

class B7 extends B implements I, J, II, JJ {
  // BAD: B, J, and JJ
  // No annotation: starts anything
  public void m() {
  }
}

class B8 extends B implements I, J, II, JJ {
  // GOOD
  @Starts("nothing")
  public void m() {
  }
}
