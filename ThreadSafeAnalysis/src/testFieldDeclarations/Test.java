package testFieldDeclarations;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.ThreadSafe;

@Region("public R")
@RegionLock("L is this protects R")
@ThreadSafe
public class Test {
  /* final field; primitive type; does satisfy ThreadSafe */
  protected final int good1 = 1;
  /* final field; non-primitive, non-declared type; does not satisfy ThreadSafe */
  protected final int[] bad1 = { 0, 1, 2 };
  
  /* final field; ThreadSafe declared type; does satisfy ThreadSafe */
  protected final Safe good2 = new Safe();
  /* final field; NonThreadSafe declared type; does not satisfy ThreadSafe */
  protected final NotSafe bad2 = new NotSafe();

  
  
  /* volatile field; primitive type; does satisfy ThreadSafe */
  protected volatile int good3 = 1;
  /* volatile field; non-primitive, non-declared type; does not satisfy ThreadSafe */
  protected volatile int[] bad3 = { 0, 1, 2 };
  
  /* volatile field; ThreadSafe declared type; does satisfy ThreadSafe */
  protected volatile Safe good4 = new Safe();
  /* volatile field; NonThreadSafe declared type; does not satisfy ThreadSafe */
  protected volatile NotSafe bad4 = new NotSafe();
  
  

  /* lock-protected field; primitive type; does satisfy ThreadSafe */
  @InRegion("R")
  protected int good5 = 1;
  /* lock-protected field; non-primitive, non-declared type; does not satisfy ThreadSafe */
  @InRegion("R")
  protected int[] bad5 = { 0, 1, 2 };

  /* lock-protected field; ThreadSafe declared type; does satisfy ThreadSafe */
  @InRegion("R")
  protected Safe good6 = new Safe();
  /* lock-protected field; NonThreadSafe declared type; does not satisfy ThreadSafe */
  @InRegion("R")
  protected NotSafe bad6 = new NotSafe();
}
