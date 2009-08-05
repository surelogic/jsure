package tt;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("public C1"),
  @Region("public C2")
})
@RegionLocks({
  @RegionLock("LL1 is l1 protects C1"),
  @RegionLock("LL2 is l2 protects C2")
})
public class C {
  public final Object l1 = new Object();
  public final Object l2 = new Object();
  
  @Unique
  @Aggregate("Instance into Instance, D1 into C1, D2 into C2")
  protected final D d = new D();
  
  @Borrowed("this")
  public void m(final boolean flag) {
    // Requires this:LL1, this:LL2
    this.d.e.doStuff();

    final D o = flag ? D.getD() : this.d;
    // Requires this:LL1, this:LL2, o:L1, o:L2 
    o.e.doStuff();
  }
}
