package test.fancy.respectsHierarchy;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
/**
 * Instance
 *   R1
 *     inR1
 *     R2
 *       R4
 *         inR4
 *     R3
 *       inR3
 */
@Regions({
  @Region("public R1"),
  @Region("public R2 extends R1"),
  @Region("public R3 extends R1"),
  @Region("public R4 extends R2")
})
public class C {
  @InRegion("R1")
  protected int inR1;
  
  @InRegion("R4")
  protected int inR4;
  
  @InRegion("R3")
  protected int inR3;
  
  @Borrowed("this")
  public C() {
  	super();
  }
}
