package test_selfProtected;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;

@RegionLock("L is this protects Instance")
public class Main {
  private C c = new C();
  private D d = new D();
  private I i = new E();
  
  @Borrowed("this")
  public Main() { }
  
  public synchronized void doIt() {
    this.c.m(); // should get "possibly shared unprotected object" warning
    this.d.m(); // thread safe class
    this.i.m(); // thread safe interface
  }
}
