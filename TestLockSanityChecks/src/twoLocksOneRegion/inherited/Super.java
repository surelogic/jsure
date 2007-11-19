package twoLocksOneRegion.inherited;

import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("public R" /* is CONSISTENT */)
/* Mark this as inconsistent because the subclass Duper doesn't protect 
 * its field usages.  This is to see if JSure says it requires the lock from
 * here or from the subclass: it should require this one.
 */
@RegionLock("L is superLock protects R" /* is INCONSISTENT */)
public class Super {
  public final Object superLock = new Object();
}
