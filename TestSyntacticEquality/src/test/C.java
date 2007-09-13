package test;

import com.surelogic.RegionLock;

/**
 * Test the syntactic equality of lock expressions.
 */
@RegionLock("MyLock is this protects Instance")
public class C {
  public int field;
  
  

  public void testFieldRef() {
    class Temp {
      public final C f1 = new C();
      public final C f2 = new C();
    }
    
    final Temp t1 = new Temp();
    final Temp t2 = new Temp();
    synchronized (t1.f1) {
      // GOOD: this should assure because t1.f1 == t1.f1
      t1.f1.field = 0;
      // BAD: this should not assure because t1.f1 != t1.f2
      t1.f2.field = 1;
      // BAD: this should not assure because t1.f1 != t2.f1
      t2.f1.field = 2;
    }
  }
  
  // TODO: How to test ClassExpression (C.class)?
  
  
  /**
   * Test ArrayRefExpresssion, as well as IntLiteral and CharLiteral
   */
  public void testArrayRefExpression() {
    final C[] array = new C[] { new C(), new C() };
    final C[] otherArray = new C[] { new C(), new C() };
    // Test with local variables
    int i = 0;
    int j = 1;
    synchronized (array[i]) {
      // GOOD: this should assure because array[i] == array[i]
      array[i].field = 0;
      // BAD: this should not assure because array[i] != arraay[j]
      array[j].field = 1;
      // BAD: this should not assure because array[i] != otherArray[i]
      otherArray[i].field = 1;
    }
    
    synchronized (array[0]) {
      // GOOD: this should assure because array[0] == array[0]
      array[0].field = 0;
      // GOOD: this should assure because array[0] == array[00]
      array[00].field = 0;
      // GOOD: this should assure because array[0] == array[0x0]
      array[0x0].field = 0;
      // GOOD: this should assure because array[0] == array['\0']
      array['\0'].field = 0;
      // GOOD: this should assure because array[0] == array['\u0000']
      array['\u0000'].field = 0;
      
      // BAD: this should not assure because array[0] != array[1]
      array[1].field = 1;
    }
  }
  
  public void testVariableUseExpression(final C c1) {
    final C c2 = new C();
    
    synchronized (c1) {
      // GOOD: c1 == c1
      c1.field = 0;
      // BAD: c1 != c2
      c2.field = 1;
    }
    
    synchronized (c2) {
      // BAD: c2 != c1
      c1.field = 0;
      // GOOD: c2 == c2
      c2.field = 1;
    }
    
    synchronized (c1) {
      // BAD: c1 != this
      this.field = 1;
    }
  }
  
  public void testReceiverExpression() {
    final C other = new C();
    
    synchronized (this) {
      // GOOD: this == this
      this.field = 1;
      // BAD: this != other
      other.field = 2;
    }
  }
  
  public void testQualifiedReceiverExpression() {
    class Inner {
      final C innerField = new C();
      @SuppressWarnings("unused")
      public void method() {
        synchronized (C.this) {
          // GOOD: C.this == C.this
          C.this.field = 1;
          // BAD: C.this != this
          this.innerField.field = 1;
        }
      }
    }
  }
  
  // TODO: How to test NamedType?
  
  /* NOTE: Cannot currently test true, false, or null because they cannot
   * appear in any legal final expression.
   */
  
  // NOTE: CharLiteral tested in testArrayRefExpression()
  
  // NOTE: IntLiteral tested in testArrayRefExpression()
}
