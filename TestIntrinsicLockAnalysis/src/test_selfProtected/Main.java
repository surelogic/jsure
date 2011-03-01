package test_selfProtected;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;

@RegionLock("L is this protects Instance")
public class Main {
  private C c = new C();
  private D d = new D();
  private E e = new E();
  private F f = new F();
  private G g = new G();
  
  @Borrowed("this")
  public Main() { }
  
  public synchronized void doIt() {
    this.c.m(); // should get "possibly shared unprotected object" warning
    this.d.m(); // selfProtected class --- 2010-10-05 Now rejected because the @ThreadSafe annotation on D doesn't pass scrubbing
    this.e.m(); // selfProtected via superclass D --- 2010-10-05 Now rejected because the @ThreadSafe annotation on D doesn't pass scrubbing
    this.f.m(); // selfProtected via interface I 
    this.g.m(); // should get "possibly shared unprotected object" warning
  }
}
