/* Created on Jun 12, 2005
 */
package inherited_region.baseCase;

/**
 * Adds fields to Instance and protects Instance.  This
 * is legal.
 * 
 * @TestResult is CONSISTENT : Adds field to Instance and protects it in the same class
 * @Lock L is this protects Instance
 */
public class Good1 {
  protected int x;
}
