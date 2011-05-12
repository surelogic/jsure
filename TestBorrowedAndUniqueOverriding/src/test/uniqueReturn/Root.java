package test.uniqueReturn;

import com.surelogic.Unique;

public class Root {
  // To be make @Unique("return") in a subclass
  public Object notOriginallyUnique() {
    return null;
  }
  
  // Kept unique in a subclass
  @Unique("return")
  public Object keptUnique1() {
    return null;
  }
  
  // Kept unique in a subclass
  @Unique("return, this")
  public Object keptUnique2() {
    return null;
  }
  
  // Not kept unique
  @Unique("return")
  public Object notKeptUnique1() {
    return null;
  }
  
  // Not kept unique
  @Unique("return, this")
  public Object notKeptUnique2() {
    return null;
  }
}

class Sub extends Root {
  // GOOD: can add unique
  @Override
  @Unique("return")
  public Object notOriginallyUnique() {
    return null;
  }
  
  // GOOD: still unique
  @Override
  @Unique("return")
  public Object keptUnique1() {
    return null;
  }
  
  // GOOD: still unique
  @Override
  @Unique("return, this")
  public Object keptUnique2() {
    return null;
  }
  
  // BAD: Not kept unique
  @Override
  public Object notKeptUnique1() {
    return null;
  }
  
  // BAD: Not kept unique
  @Override
  @Unique("this")
  public Object notKeptUnique2() {
    return null;
  }
}

class K {
  public Object foo() {
    return null;
  }
}

interface I1 {
  public Object foo();
}

interface I2 {
  public Object foo();
}

interface I3 {
  @Unique("return")
  public Object foo();
}

interface I4 {
  @Unique("return")
  public Object foo();
}


class X implements I1, I3 {
  // BAD: Must be unique, per I3
  public Object foo() {
    return null;
  }
}


class Y extends K implements I1, I2, I3, I4 {
  // BAD: Must be unique, per I3, I4
  @Override
  public Object foo() {
    return null;
  }
}
