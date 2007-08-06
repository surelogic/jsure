/*
 * Created on Nov 14, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package test_unique_names;

import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.Region;
import com.surelogic.Regions;

/**
 * There are five lock declarations.  Checking goes first by policy locks
 * in declaration order, and then state locks in declaration order.  State
 * and policy locks share the same name space.
 */
@Regions({
  @Region("A"),
  @Region("B")
})
@PolicyLocks({
  @PolicyLock("L is class" /*is CONSISTENT: First use of L (Policy)*/),
  @PolicyLock("P is class" /*is CONSISTENT: First use of P (Policy)*/),
  @PolicyLock("P is class" /*is UNASSOCIATED: Second use of P (Policy)*/)
})
@Locks({
  @Lock("L is this protects A" /*is UNASSOCIATED: Second use of L (State)*/),
  @Lock("L is this protects B" /*is UNASSOCIATED: Third use of L (State)*/)
})
public class StateVsPolicyLocks {
  // empty body
}
