package test_unique_names;

/**
 * Bad: Reuses lock names L1 and P1
 * 
 * @region public R3
 *
 * @lock L1 is this protects R3
 * @policyLock P1 is class
 */
public class BadC3 extends GoodC2 {

}
