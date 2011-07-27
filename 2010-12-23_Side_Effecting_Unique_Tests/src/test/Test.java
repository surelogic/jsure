package test;
import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;


public class Test {
  private @Unique Object u;

  private Object f;
  
  
  
  @RegionEffects("none")
  private static void compromiseRef(final Object o) {
    // do nothing
  }
  
  @Borrowed("this")
  public void m1() {
    // Trigger borrowed error in opCompromiseNoRelease
    compromiseRef(this); // bad, on normal and abrupt paths
  }
  
  public void m2(@Borrowed Object p) {
    // Trigger borrowed error in opCompromiseNoRelease
    compromiseRef(p); // bad, on normal and abrupt paths
  }
  
  @Borrowed("this")
  public Object r1() {
    // Trigger borrowed error in opCompromiseNoRelease
    return this; // bad
  }
  
  public Object r2(@Borrowed Object p) {
    // Trigger borrowed error in opCompromiseNoRelease
    return p; // bad
  }
  
  @Borrowed("this")
  public void f1() {
    // Trigger borrowed error in opCompromiseNoRelease
    this.f = this; // bad
  }
  
  public void f2(@Borrowed Object p) {
    // Trigger borrowed error in opCompromiseNoRelease
    this.f = p; // bad
  }

  public void good(@Borrowed Object p) {
    System.out.println("Doesn't use p");
  }
  
  
  public void undefined1() {
    final Object t1 = this.u;
    final Object t2 = this.u;
    // Trigger undefined error in opGet() 
    System.out.println(t1);
  }

  public Object undefined2() {
    final Object t1 = this.u;
    final Object t2 = this.u;
    // Trigger undefined error in opGet() 
    return t1; 
  }

  public void undefined3() {
    final Object t1 = this.u;
    final Object t2 = this.u;
    // Trigger undefined error in opGet() 
    this.f = t1;
  }
  
  private void t(final @Unique Object u, final Object o) {
  }

  public void undefined4() {
    final Object z = this.u;
    // Trigger undefined error in opCompromiseNoRelease() 
    t(z, z);
  }
  
  
  
  @Unique("return")
  public Object uniqueReturn1(@Borrowed Object b) {
    // BAD: Borrowed return
    // Trigger borrowed error in opUndefine() via transferMethodBody()
    return b;
  }
  
  @Borrowed("this")
  public Object uniqueReturn1b(@Borrowed String b) {
    // BAD: Borrowed return
    // Trigger borrowed error in opCompromiseNoRelease() via transferMethodBody()
    return this;
  }
  
  @Unique("return")
  public Object uniqueReturn2(Object s) {
    // BAD: Shared return
    // Trigger shared error in opUndefine() via transferMethodBody()
    return s;
  }
  
  @Unique("return")
  public Object uniqueReturn3(@Unique Object u) {
    // Good
    return u;
  }
  
  @Unique("return")
  public Object uniqueReturn4() {
    // Good
    return new Object();
  }
  
  @Unique("return")
  public Object uniqueReturn5() {
    Object a = this.u;
    Object b = this.u;
    // No way to trigger undefined error in opUndefine() via transferMethodBody()
    // Bad: Shows up as reading an undefined variable
    return a;
  }


  @RegionEffects("none")
  private void needsUnique(Object other, @Unique Object u) {
    // blah
  }
  
  public void uniqueParam1(@Borrowed Object b) {
    // BAD: Borrowed param
    // Trigger borrowed error in opUndefine() via popArguments()
    needsUnique(null, b);
  }
  
  public void uniqueParam2(Object s) {
    // BAD: Shared param
    // Trigger shared error in opUndefine() via popArguments()
    needsUnique(null, s);
  }
  
  public void uniqueParam3(@Unique Object u) {
    // Good
    needsUnique(null, u);
  }
  
  public void uniqueParam4() {
    // Good
    needsUnique(null, new Object());
  }

  public void uniqueParam5(@Unique Object u) {
    // bad: undefined
    Object x = this.u;
    compromiseRef(x);
    // No way to trigger undefined error in opUndefine() via transferMethodBody()
    // Shows up as load of a undefined field
    needsUnique(x, this.u);
  }
  
  
  
  
  private class C {
    @Unique("return")
    public C() {}
    
    @Unique("this")
    public Object m() { return null; }
    
    @Unique("this")
    public void n(Object o) {}
  }
  
  @Unique
  private C uniqueC;
  
  public void uniqueReceiver1(@Borrowed C b) {
    // bad: receiver is borrowed
    // Trigger borrowed error in opUndefine() via popReceiver()
    b.m();
  }

  public void uniqueReceiver2(C s) {
    // bad: receiver is aliased
    // Trigger shared error in opUndefine() via popReceiver()
    s.m();
  }
  
  public void uniqueReceiver3(@Unique C u) {
    // good: receiver is unique
    u.m();
  }
  
  public void uniqueReciever4() {
    // good: receiver is unique
    (new C()).m();
  }
  
  public void uniqueReceiver5() {
    // bad: undefined
    C x = uniqueC;
    x.n(
        // Trigger undefined error in opUndefine() via popReceiver()
        x.m());
  }
  
  
  public void assignToUniqueFieldMakesUnshared(@Unique Object u) {
    Object x = this.u = u;    // Originally: x is undefined; NOW: this.u is compromised
    /* Used to Trigger error in opGet() via transferUseVar() because the assignment made 'x' undefined.  But now we treat the assignment as reading this.u, so 'u' becomes a compromised field that is not restored at the end of the method. */
    needsUnique(null, x);
  }
  
  
  
  
  public void loadCompromisedUniqueField1() {
    Object a = this.u; // fine
    compromiseRef(a); // u is trashed
    // Trigger error in opLoad() via transferUseFied()
    Object b = this.u; // Load of compromised field
  }

  @RegionEffects("reads u")
  private void readU() {}
  
  public void loadCompromisedUniqueField2() {
    compromiseRef(this.u); // trash u
    // Trigger error in opLoad() via considerEffects()
    this.readU(); // indirectly read u, load of compromised field
  }
  
  
  
  @RegionEffects("reads java.lang.Object:All")
  private static void doesAnything(Object o) {}
  
  public void foo(Test other) {
    // Trigger error via the SHARED pseudo variable
    compromiseRef(other.u);
    doesAnything(other); // trigger error in opLoadReachable() via considerEffects()
  }
  
  
  
  @RegionEffects("reads x:Instance")
  private static void readsInstance(final Test x) {}
  
  @Unique("this")
  public void bad1(@Unique Test other1, @Unique Test other2) {
    compromiseRef(other1.u);
    readsInstance(other1); // trigger error in opLoadReachable() via considerEffects()
  }
  
  @Unique("this")
  public void bad2(@Unique Test other1, @Unique Test other2) {
    // Test that different parameters are distinguished by the error
    compromiseRef(other1.u);
    readsInstance(this); // okay
    readsInstance(other2); // okay
    readsInstance(other1); // trigger error in opLoadReachable() via considerEffects()
  }
  
  @Unique("this")
  public void bad3(@Unique Test other1, @Unique Test other2) {
    // Test that different parameters are distinguished by the error
    compromiseRef(other2.u);
    readsInstance(this); // okay
    readsInstance(other1); // okay
    readsInstance(other2); // trigger error in opLoadReachable() via considerEffects()
  }
  
  @Unique("this")
  public void bad4(@Unique Test other1, @Unique Test other2) {
    // Test that receiver is properly recognized and handled as actual argument
    compromiseRef(this.u);
    readsInstance(this); // trigger error in opLoadReachable() via considerEffects()
  }


  
  @RegionEffects("reads this:Instance")
  @Borrowed("this")
  private void readsInstance() {}
  
  
  @Unique("this")
  public void bad5(@Unique Test other1, @Unique Test other2) {
    // Test that receiver is properly handled as the formal parameter AND as actual
    compromiseRef(this.u);
    this.readsInstance(); // trigger error in opLoadReachable() via considerEffects()
  }
  
  @Unique("this")
  public void bad6(@Unique Test other1, @Unique Test other2) {
    // Test that receiver is properly handled as the formal parameter and that other variables can be used as the actual 
    compromiseRef(other1.u);
    this.readsInstance(); // good
    other2.readsInstance(); // good
    other1.readsInstance(); // trigger error in opLoadReachable() via considerEffects()
  }
  
  @Unique("this")
  public void bad7(@Unique Test other1, @Unique Test other2) {
    // Test that receiver is properly handled as the formal parameter and that other variables can be used as the actual 
    compromiseRef(other2.u);
    this.readsInstance(); // good
    other1.readsInstance(); // good
    other2.readsInstance(); // trigger error in opLoadReachable() via considerEffects()
  }
  
  @Unique("this")
  public void bad8(@Unique Test other1, @Unique Test other2) {
    // Test that receiver is properly handled as the formal parameter AND as actual
    compromiseRef(this.u);
    other1.readsInstance(); // good
    other2.readsInstance(); // good
    this.readsInstance(); // trigger error in opLoadReachable() via considerEffects()
  }

  public void badUniqueAssignement1(final Object notUnique) {
    // Trigger "Shared value on stack not unique" in opUndefine via opStore()
    this.u = notUnique;
  }

  public void badUniqueAssignement2() {
    // Trigger "Shared value on stack not unique" in opUndefine via opStore()
    this.u = "not unique";
  }

  public void badUniqueAssignement3() {
    // Trigger "Shared value on stack not unique" in opUndefine via opStore()
    this.u = notUnique();
  }
  
  private Object notUnique() { return null; }
  
  private void assignBorrowedToUnique(final @Borrowed String borrowed) {
    // Trigger "Borrowed value on stack not unique" in opUndefine via opStore()
    this.u = borrowed;
  }
  
  public void uniqueReceiver6() {
    this.uniqueC.n(
        // Trigger undefined error in opUndefine() via popReceiver()
        this.uniqueC.m());
  }
  
  // Borrowed dominates at the end of method, so just have an error about borrowed return value
  @Unique("return")
  public Object reallyBadUniqueReturn(final boolean flag,
      final Object shared, final @Borrowed Object borrowed) {
    if (flag) {
      return shared;
    } else {
      return borrowed;
    }
  }
}


