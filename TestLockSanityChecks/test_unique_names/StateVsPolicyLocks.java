/*
 * Created on Nov 14, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package test_unique_names;

/**
 * There are five lock declarations.  Checking goes first by policy locks
 * in declaration order, and then state locks in declaration order.  State
 * and policy locks share the same name space.
 * 
 * @Region A
 * @Region B
 *
 * @TestResult is CONSISTENT: First use of L (Policy)
 * @PolicyLock L is class
 * @TestResult is CONSISTENT: First use of P (Policy)
 * @PolicyLock P is class
 * @TestResult is UNASSOCIATED: Second use of P (Policy)
 * @PolicyLock P is class
 * @TestResult is UNASSOCIATED: Second use of L (State)
 * @Lock L is this protects A
 * @TestResult is UNASSOCIATED: Third use of L (State)
 * @Lock L is this protects B
 */
public class StateVsPolicyLocks {
  // empty body
}
