package test_unique_names;

/**
 * Bad: Reuses lock names L1 and P1
 * 
 * @region public R2
 *
 * @lock L1 is this protects R2
 * @policyLock P1 is class
 */
public class BadC2 extends C1 {

}
