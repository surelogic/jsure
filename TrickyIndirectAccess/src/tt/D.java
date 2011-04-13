package tt;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("public D1"),
  @Region("public D2")
})
@RegionLocks({
  @RegionLock("L1 is l1 protects D1"),
  @RegionLock("L2 is l2 protects D2")
  
})
public class D {
  public final Object l1 = new Object();
  public final Object l2 = new Object();
  
  @UniqueInRegion("Instance into Instance, f1 into D1, f2 into D2")
  protected final E e = new E();
  
  public static D getD() {
    return new D();
  }
  
  @Borrowed("this")
  public D() {
    super();
  }
  
  public void m(final boolean flag) {
    // Needs this:L1, this:L2
    this.e.doStuff();

    final E o = flag ? E.getE() : this.e;    
    // Needs this:L1, this:L2
    o.doStuff();
  }
}
