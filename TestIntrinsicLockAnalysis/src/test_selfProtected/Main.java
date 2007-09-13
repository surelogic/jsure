package test_selfProtected;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.SingleThreaded;

@RegionLock("L is this protects Instance")
public class Main {
  private C c = new C();
  private D d = new D();
  private E e = new E();
  private F f = new F();
  private G g = new G();
  
  @SingleThreaded
  @Borrowed("this")
  public Main() { }
  
  public synchronized void doIt() {
    this.c.m(); // should get "possibly shared unprotected object" warning
    this.d.m(); // selfProtected class
    this.e.m(); // selfProtected via superclass D
    this.f.m(); // selfProtected via interface I 
    this.g.m(); // should get "possibly shared unprotected object" warning
  }
}
