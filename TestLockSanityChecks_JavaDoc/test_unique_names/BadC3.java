package test_unique_names;

/**
 * Bad: Reuses lock names L1 and P1
 * 
 * @Region public R3
 *
 * @TestResult is UNASSOCIATED: L1 is inherited from grandparent
 * @RegionLock L1 is this protects R3
 * @TestResult is UNASSOCIATED: P1 is inherited from grandparent
 * @PolicyLock P1 is class
 */
public class BadC3 extends GoodC2 {

}
