/* Created on Jun 12, 2005
 */
package inherited_region.Instance;

/**
 * BAD: cannot protect Instance because it has fields in a super class.
 * @lock L is this protects Instance
 */
public class Bad1 extends BadRoot1 {

}
