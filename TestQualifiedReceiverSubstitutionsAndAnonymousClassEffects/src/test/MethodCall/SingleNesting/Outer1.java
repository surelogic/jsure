package test.MethodCall.SingleNesting;

import com.surelogic.RegionEffects;

public class Outer1 {
  public int f1;
  public int f2;

  public Outer1(final int x) {
    f1 = x;
    f2 = x + 1;
  }

  public class Nested {
    public int f3;
    
    public Nested(final int y) {
      f3 = y;
    }
    
    @RegionEffects("reads this:f3, Outer1.this:f1, Outer1.this:f2")
    public int doStuff() {
      return
        this.f3 +
        Outer1.this.f1 +
        Outer1.this.f2;
    }
    
    @RegionEffects("reads this:f3, any(Outer1):f1, any(Outer1):f2")
    public int testReceiverIsThis() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      return this.doStuff();
    }
  }
  
  public class E1 extends Nested {
    public E1(final int y) {
      super(y);
    }
    
    @RegionEffects("reads this:f3, Outer1.this:f1, Outer1.this:f2")
    public int testReceiverIsThis2() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      return this.doStuff();
    }
    
    @RegionEffects("reads this:f3, Outer1.this:f1, Outer1.this:f2")
    public int testReceiverIsSuper() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      return super.doStuff();
    }
  }
  
  
  @RegionEffects("reads n1:f3, n2:f3, any(Outer1):f1, any(Outer1):f2")
  public static int test(final Nested n1, final Nested n2) {
    return
      // Cannot map qualified receiver: use any instance target
      n1.doStuff() +
      // Cannot map qualified receiver: use any instance target
      n2.doStuff();
  }
}

