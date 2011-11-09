package test.ConstructorCall.Qualified.DoubleNesting;

import com.surelogic.RegionEffects;

public class Outer1 {
  public int f1;

//  public static void main(String[] args) {
//    final Outer1 o1 = new Outer1();
//    final Nested1 n1 = o1. new Nested1();
//    final Nested1.Nested2 nn1 = n1. new Nested2(5);
//    System.out.println("o1 = " + o1);
//    System.out.println("n1 = " + n1);
//    
//    final Outer1 o2 = new Outer1();
//    final E1 e1 = o2. new E1(n1, 10);
//    System.out.println("o2 = " + o2);
//    System.out.println("n1 = " + n1);
//  }
  
  public Outer1() {
    f1 = 0;
  }

  public class Nested1 {
    public int f2;
    
    public Nested1() {
      f2 = 1;
    }
    
    public class Nested2 {
      public int f3;
      
      @RegionEffects("writes Outer1.this:f1, test.ConstructorCall.Qualified.DoubleNesting.Outer1.Nested1.this:f2")
      public Nested2(final int z) {
        this.f3 = z;
        Nested1.this.f2 = z + 1;
        Outer1.this.f1 = z + 2;
//        System.out.println("** Nested1.this = " + Nested1.this);
//        System.out.println("** Outer1.this = " + Outer1.this);
      }
    }
  }  

  public class E1 extends Nested1.Nested2 {
    @RegionEffects("writes any(Outer1):f1, n:f2")
    public E1(final Nested1 n, final int y) {
      /* Effects on qualified receiver Nested1.this are mapped to effects on n.
       * Effects on qualified receiver Outer1.this are mapped to any instance
       * of Outer1, because the Outer1 object modified by super() is the
       * enclosing instance of n, the identity of which is not known here.
       */
      n. super(y);
      // Affects OUR Outer.this, not the Outer1.this of n
      Outer1.this.f1 = 10;
//      System.out.println("* n = " + n);
//      System.out.println("* Outer1.this = " + Outer1.this);
    }

    @RegionEffects("writes any(Outer1):f1, oo:f2")
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

