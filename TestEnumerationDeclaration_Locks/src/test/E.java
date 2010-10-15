package test;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("L is this protects Instance"),
  @RegionLock("S is class protects Crap"),
  @RegionLock("SS is class protects Junk")
})
@Regions({
  @Region("private static Crap"),
  @Region("private static Junk")
})
public enum E {
  /* Need to make sure that we treat this as a constructor call, and that we
   * consider the RequireLock annotation on E().  
   */
  A,
  
  /* Need to make sure that we visit the argument list.  Call to getSecret()
   * succeeds because we are in a static context. 
   * 
   * Need to make sure that we treat this as a constructor call, and that we
   * consider the RequireLock annotations on E(int).
   */
  B(getSecret(0) /* Used to get errors here because the analysis context was not properly set in LockVisitor.  This is part of bug 1627 */),

  /* Need to make sure that we visit the argument list.  Call to getSecret()
   * succeeds because we are in a static context. 
   * 
   * Need to make sure that we treat this as a constructor call, and that we
   * consider the RequiresLock annotation on E(int).
   */
  C(getSecret(1) /* Used to get errors here because the analysis context was not properly set in LockVisitor.  This is part of bug 1627 */) {
    {
      // Should be flagged as unprotected
      /* Need to process the enum constant declaration as a class declaration
       * and visit the class's initializers to reach here.  This is done when
       * processing the declaration of the enumeration constant itself. 
       * This is part of bug 1627.
       */
      secret += 100;
    }
    
    @Override
    public void foo() {
      // Should be flagged as unprotected
      /* Need to visit the contents of the class declaration to reach here.
       * That is, the top level assurance drivers need to visit the class bodies
       * of EnumConstantClassDeclaration nodes.  This is part of bug 1627.
       */
      secret += 10;
    }
  };

  protected int secret;

  @RequiresLock("SS")
  private E() {
    this(-1);
  }
  
  @RequiresLock("SS")
  private E(final int v) {
    // Should be flagged as unprotected
    /* Need to visit the class body of enumerations to reach here.  This was
     * already being done, and is not part of bug 1627.
     */
    secret = v;
  }
  
  @RequiresLock("S")
  protected static int getSecret(final int v) {
    return v + 1;
  }
  
  /* Need this so that C can override something */
  public void foo() {
    // do nothing
  }
}
