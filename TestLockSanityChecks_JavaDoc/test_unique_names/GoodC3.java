package test_unique_names;

/**
 * @Region public R3
 *
 * @TestResult is CONSISTENT: First use of L3 in hierarchy
 * @RegionLock L3 is this protects R3
 * @TestResult is CONSISTENT: First use of P3 in hierarchy
 * @PolicyLock P3 is class
 */
public class GoodC3 extends GoodC2 {

}
