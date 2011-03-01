package test.ConstructorCall.Qualified.DoubleNesting;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("F1 is lockF1 protects f1")
public class Outer1 {
  public final Object lockF1 = new Object();
  public int f1;

  @Borrowed("this")
  public Outer1() {
    f1 = 0;
  }

  @RegionLock("F2 is lockF2 protects f2")
  public class Nested1 {
    public final Object lockF2 = new Object();
    public int f2;
    
    @Borrowed("this")
    public Nested1() {
      f2 = 1;
    }
    
    @RegionLock("F3 is lockF3 protects f3")
    public class Nested2 {
      public final Object lockF3 = new Object();
      public int f3;
      
      @RequiresLock("Outer1.this:F1, test.ConstructorCall.Qualified.DoubleNesting.Outer1.Nested1.this:F2")
      @Borrowed("this")
      public Nested2(final int z) {
        this.f3 = z;
        Nested1.this.f2 = z + 1;
        Outer1.this.f1 = z + 2;
      }
    }
  }  

  public class E1 extends Nested1.Nested2 {
    @RequiresLock("n:F2")
    public E1(final Nested1 n, final int y) {
      /* Effects on qualified receiver Nested1.this are mapped to effects on n.
       * Effects on qualified receiver Outer1.this are mapped to any instance
       * of Outer1, because the Outer1 object modified by super() is the
       * enclosing instance of n, the identity of which is not known here.
       */
      n. super(y); // Cannot resolve F1
    }

    @RequiresLock("Outer1.this:F1, oo:F2")
    public E1(final Nested1 oo) {
      /* Effects on qualified receiver Outer1.this are mapped to any instance
       * of Outer1, because our Outer1 is the enclosing instance of oo, the
       * identity of which is not known here.
       */
      this(oo, 10);
      // Affects OUR Outer.this, not the Outer1.this of n
      Outer1.this.f1 = 10;
    }
  }
}

