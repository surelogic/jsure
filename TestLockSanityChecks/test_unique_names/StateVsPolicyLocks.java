/*
 * Created on Nov 14, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package test_unique_names;

/**
 * There are five lock declarations.  Checking goes first by state locks
 * in declaration order, and then poliyc locks in declaration order.  State
 * and policy locks share the same namespace.
 * 
 * (1) Good---first use of "L" (as state lock)
 * (2) Bad---reuse of "L" as a state lock
 * (3) Bad---reuse of "L" as a policy lock
 * (4) Good---First use of "P" (as a policy lock)
 * (5) Bad---reuse of "P" as a policy lock
 * @region A
 * @region B
 * 
 * @lock L is this protects A
 * @lock L is this protects B
 * @policyLock L is class
 * @policyLock P is class
 * @policyLock P is class
 */
public class StateVsPolicyLocks {
  // empty body
}
