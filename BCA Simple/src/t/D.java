package t;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.Unique;

/**
 * Simple class that aggregates a uniquely referenced E object into its own
 * protected state.
 */

@Regions({
  @Region("protected D_R1"),
  @Region("protected D_R2")
})
@RegionLock("DLock is this protects D_R2")
public class D {
  @Unique
  @Aggregate("f1 into D_R1, f2 into D_R2, Instance into Instance")
  protected final E e = new E();
  
  @Borrowed("this")
  @RegionEffects("none")
  public D() {
    super();
  }
  
  @Borrowed("this")
  protected synchronized void doStuff1(final int v1, final int v2) {
    /* Access the state of the E object directly.  Lock analysis
     * understands we need the lock for this.D_R2.  We have always
     * checked this correctly.
     */
    this.e.f1 = v1;
    this.e.f2 = v2;
  }
  
  
  @Borrowed("this")
  protected synchronized void doStuff2(final int v1, final int v2) {
    /* Access the state of the E object directly, but conceal the use of the
     * field 'e' via use of a local variable.  Lock analysis
     * understands we need the lock for this.D_R2.  Until July 30, 2009, we
     * did not catch this.
     */
    final E local_e = this.e;
    local_e.f1 = v1;
    local_e.f2 = v2;
  }  
  
  protected static void extra(final D other2) {
    /* Make sure indirect access works correct for non-receiver references.
     * Here we also test that we partially undo the work of elaboration.  If
     * we do no undo elaboration, the needed lock will reference the
     * variable declarations of other and other2 instead of containing the 
     * VariableUseExpressions that we traditionally have used in the locks.
     */
    final D other = new D();
    synchronized (other) {
      other.e.f2 = 10;
      other.e.doStuff(10, 11);
    }
    
    synchronized (other2) {
      other2.e.f2 = 10;
      other2.e.doStuff(10, 11);
    }
    
  }
}
