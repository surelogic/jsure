/* Created on Jun 12, 2005
 */
package inherited_region.Subregion;

/**
 * BAD: cannot protect Instance because it (indirectly) has fields in a super class.
 * @lock L is this protects Instance
 */
public class Bad2 extends BadRoot2 {

}
