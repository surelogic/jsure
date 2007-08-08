/* Created on Jun 12, 2005
 */
package inherited_region.baseCase;

import com.surelogic.Lock;

/**
 * Adds fields to Instance and protects Instance.  This
 * is legal.
 */

// TestResult is CONSISTENT : Adds field to Instance and protects it in the same class
@Lock("L is this protects Instance" /* is CONSISTENT : Adds field to Instance and protects it in the same class */)
public class Good1 {
  protected int x;
}
