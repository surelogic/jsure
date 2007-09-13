package test_unique_names;

/**
 * @Region public R1
 *
 * @TestResult is CONSISTENT: First use of L1 in hierarchy
 * @RegionLock L1 is this protects R1
 * @TestResult is CONSISTENT: First use of P1 in hierarchy
 * @PolicyLock P1 is class
 */
public class C1 {

}
