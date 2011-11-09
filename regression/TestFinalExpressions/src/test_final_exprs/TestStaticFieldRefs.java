package test_final_exprs;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;


/**
 * Tests the finalness or not of static field references.
 */
@Region("static StaticRegion")
@RegionLock("StaticLock is class protects StaticRegion")
public class TestStaticFieldRefs {
  private static final Object staticFinalField = new Object();
  private static Object staticUnprotectedField = new Object();
  
  @InRegion("StaticRegion")
  private static Object staticProtectedField = new Object();
  
  public void good_staticFinalField() {
    // FINAL: field is static and final
    // Unidentifiable lock expression
    synchronized (staticFinalField) {
      // do stuff
    }
  }
  
  public void bad_staticNonfinalFieldUnprotected() {
    // NON-FINAL: field is static, but non final and unprotected
    synchronized (staticUnprotectedField) {
      // do stuff
    }
  }

  public void bad_staticNonfinalFieldProtectedReadonly_unprotected() {
    // FINAL: field is static, non-final but unprotected, and is read only in the synchronized block
    // BAD: unprotected field reference
    // Unidentifiable lock expression
    synchronized (staticProtectedField) {
      readField_staticProtectedField();
    }
  }

  public void good_staticNonfinalFieldProtectedReadonly_protected() {
    synchronized (TestStaticFieldRefs.class) {
      // FINAL: field is static, non-final but protected, and is read only in the synchronized block
      // GOOD: protected field reference
      // Unidentifiable lock expression
      synchronized (staticProtectedField) {
        readField_staticProtectedField();
      }
    }
  }

  public void bad_staticNonfinalFieldProtectedWrittenTo_unprotected() {
    // NON-FINAL: field is static, non-final but protected, but is WRITTEN to by the synchronized block
    // BAD: unprotected field reference
    synchronized (staticProtectedField) {
      // BAD: unprotected field reference
      staticProtectedField = new Object();
    }
  }

  public void bad_staticNonfinalFieldProtectedWrittenTo_protected() {
    synchronized (TestStaticFieldRefs.class) {
      // NON-FINAL: field is static, non-final but protected, but is WRITTEN to by the synchronized block
      // GOOD: protected field reference
      synchronized (staticProtectedField) {
        // GOOD: protected field reference
        staticProtectedField = new Object();
      }
    }
  }
  
  
  
  @RegionEffects("reads test_final_exprs.TestStaticFieldRefs:staticProtectedField")
  private void readField_staticProtectedField() {
    // do stuff
  }
}
