package test_unique_names;

/**
 * Bad: Reuses lock names L1 and P1
 * 
 * @Region public R2
 *
 * @TestResult is UNASSOCIATED: L1 is already inherited from C1
 * @Lock L1 is this protects R2
 * @TestResult is UNASSOCIATED: P1 is already inherited from C1
 * @PolicyLock P1 is class
 */
public class BadC2 extends C1 {

}
