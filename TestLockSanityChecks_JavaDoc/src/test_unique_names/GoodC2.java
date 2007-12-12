package test_unique_names;

/**
 * @Region public R2
 *
 * @TestResult is CONSISTENT: First use of L2 in hierarchy
 * @RegionLock L2 is this protects R2
 * @TestResult is CONSISTENT: First use of P2 in hierarchy
 * @PolicyLock P2 is class
 */
public class GoodC2 extends C1 {

}
