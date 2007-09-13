package test_final_exprs;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Reads;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.SingleThreaded;
import com.surelogic.Writes;


/**
 * Tests the finalness or not of instance field references.
 */
@Region("InstanceRegion")
@RegionLock("InstanceLock is this protects InstanceRegion")
public class TestInstanceFieldRefs {
  private final Object finalField = new Object();
  private Object unprotectedField = new Object();
  
  @InRegion("InstanceRegion")
  private Object protectedField = new Object();
  
  
  @SingleThreaded
  @Borrowed("this")
  public TestInstanceFieldRefs() {
    // Needed to protect the initialization of "protectedField"
  }
  
  
  
  public void good_finalField_implicitThis() {
    // FINAL: field is final and the object expression is final: implicit "this"
    // Should get unidentifiable lock expression
    synchronized (finalField) {
      // do stuff
    }
  }
  
  public void good_finalField_explicitThis() {
    // FINAL: field is final and the object expression is final: explicit "this"
    // Should get unidentifiable lock expression
    synchronized (this.finalField) {
      // do stuff
    }
  }
  
  public void good_finalField_finalParameter(final TestInstanceFieldRefs p) {
    // FINAL: field is final and the object expression is final: final parameter "p"
    // Should get unidentifiable lock expression
    synchronized (p.finalField) {
      // do stuff
    }
  }
  
  public void good_finalField_effectivelyFinalParameter(TestInstanceFieldRefs p) {
    /* FINAL: field is final and the object expression is final: parameter "p"
     * is effectively final because it is not modified inside of the synchronized
     * block.
     */
    // Should get unidentifiable lock expression
    synchronized (p.finalField) {
      // do stuff (but do not write p)
    }
  }

  
  
  public void bad_nonfinalFieldUnprotected_implicitThis() {
    /* NON-FINAL: field is non-final and unprotected; 
     * the object expression is final: implicit "this"
     */
    synchronized (unprotectedField) {
      // do stuff
    }
  }
  
  public void bad_nonfinalFieldUnprotected_explicitThis() {
    /* NON-FINAL: field is non-final and unprotected; 
     * the object expression is final: explicit "this"
     */
    synchronized (this.unprotectedField) {
      // do stuff
    }
  }
  
  public void bad_nonfinalFieldUnprotected_finalParameter(final TestInstanceFieldRefs p) {
    /* NON-FINAL: field is non-final and unprotected; 
     * the object expression is final: final parameter "p"
     */
    synchronized (p.unprotectedField) {
      // do stuff
    }
  }
  
  public void bad_nonfinalFieldUnprotected_effectivelyFinalParameter(TestInstanceFieldRefs p) {
    /* NON-FINAL: field is non-final and unprotected; 
     * the object expression is final: parameter "p"
     * is effectively final because it is not modified inside of the synchronized
     * block.
     */
    synchronized (p.unprotectedField) {
      // do stuff (but do not write p)
    }
  }

  
  
  public void good_nonfinalFieldProtectedReadonly_implicitThis_unprotected() {
    /* FINAL: field is non-final and protected and is read-only in the
     * synchronized block; the object expression is final: implicit "this"
     */
    // BAD: unprotected field reference
    // Should have unidentifiable lock warning
    synchronized (protectedField) {
      // do stuff
      this.readInstance();
    }
  }
  
  public void good_nonfinalFieldProtectedReadonly_explicitThis_unprotected() {
    /* FINAL: field is non-final and protected and is read-only in the
     * synchronized block; the object expression is final: explicit "this"
     */
    // BAD: unprotected field reference
    // Should have unidentifiable lock warning
    synchronized (this.protectedField) {
      // do stuff
      this.readInstance();
    }
  }
  
  public void good_nonfinalFieldProtectedReadonly_finalParameter_unprotected(final TestInstanceFieldRefs p) {
    /* FINAL: field is non-final and protected and is read-only in the
     * synchronized block; the object expression is final: final parameter "p"
     */
    // BAD: unprotected field reference
    // Should have unidentifiable lock warning
    synchronized (p.protectedField) {
      // do stuff
      p.readInstance();
    }
  }
  
  public void good_nonfinalFieldProtectedReadonly_effectivelyFinalParameter_unprotected(TestInstanceFieldRefs p) {
    /* FINAL: field is non-final and protected and is read-only in the
     * synchronized block; the object expression is final:  parameter "p"
     * is effectively final because it is not modified inside of the synchronized
     * block.
     */
    // BAD: unprotected field reference
    // Should have unidentifiable lock warning
    synchronized (p.protectedField) {
      // do stuff (but do not write p)
      p.readInstance();
    }
  }

  
  
  public synchronized void good_nonfinalFieldProtectedReadonly_implicitThis_protected() {
    /* FINAL: field is non-final and protected and is read-only in the
     * synchronized block; the object expression is final: implicit "this"
     */
    // GOOD: protected field reference
    // Should have unidentifiable lock warning
    synchronized (protectedField) {
      // do stuff
      this.readInstance();
    }
  }
  
  public synchronized void good_nonfinalFieldProtectedReadonly_explicitThis_protected() {
    /* FINAL: field is non-final and protected and is read-only in the
     * synchronized block; the object expression is final: explicit "this"
     */
    // GOOD: protected field reference
    // Should have unidentifiable lock warning
    synchronized (this.protectedField) {
      // do stuff
      this.readInstance();
    }
  }
  
  public void good_nonfinalFieldProtectedReadonly_finalParameter_protected(final TestInstanceFieldRefs p) {
    synchronized (p) {
      /* FINAL: field is non-final and protected and is read-only in the
       * synchronized block; the object expression is final: final parameter "p"
       */
      // GOOD: protected field reference
      // Should have unidentifiable lock warning
      synchronized (p.protectedField) {
        // do stuff
        p.readInstance();
      }
    }
  }
  
  public void good_nonfinalFieldProtectedReadonly_effectivelyFinalParameter_protected(TestInstanceFieldRefs p) {
    synchronized (p) {
      /* FINAL: field is non-final and protected and is read-only in the
       * synchronized block; the object expression is final:  parameter "p"
       * is effectively final because it is not modified inside of the synchronized
       * block.
       */
      // GOOD: protected field reference
      // Should have unidentifiable lock warning
      synchronized (p.protectedField) {
        // do stuff (but do not write p)
        p.readInstance();
      }
    }
  }

  
  
  public void bad_nonfinalFieldProtectedWrittenTo_implicitThis_unprotected() {
    /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
     * synchronized block; the object expression is final: implicit "this"
     */
    // BAD: unprotected field reference
    synchronized (protectedField) {
      // do stuff
      this.writeInstance();
    }
  }
  
  public void bad_nonfinalFieldProtectedWrittenTo_explicitThis_unprotected() {
    /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
     * synchronized block; the object expression is final: explicit "this"
     */
    // BAD: unprotected field reference
    synchronized (this.protectedField) {
      // do stuff
      this.writeInstance();
    }
  }
  
  public void bad_nonfinalFieldProtectedWrittenTo_finalParameter_unprotected(final TestInstanceFieldRefs p) {
    /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
     * synchronized block; the object expression is final: final parameter "p"
     */
    // BAD: unprotected field reference
    synchronized (p.protectedField) {
      // do stuff
      p.writeInstance();
    }
  }
  
  public void bad_nonfinalFieldProtectedWrittenTo_effectivelyFinalParameter_unprotected(TestInstanceFieldRefs p) {
    /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
     * synchronized block; the object expression is final:  parameter "p"
     * is effectively final because it is not modified inside of the synchronized
     * block.
     */
    // BAD: unprotected field reference
    synchronized (p.protectedField) {
      // do stuff 
      p.writeInstance();
    }
  }

  
  
  public synchronized void bad_nonfinalFieldProtectedWrittenTo_implicitThis_protected() {
    /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
     * synchronized block; the object expression is final: implicit "this"
     */
    // GOOD: protected field reference
    synchronized (protectedField) {
      // do stuff
      this.writeInstance();
    }
  }
  
  public synchronized void bad_nonfinalFieldProtectedWrittenTo_explicitThis_protected() {
    /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
     * synchronized block; the object expression is final: explicit "this"
     */
    // GOOD: protected field reference
    synchronized (this.protectedField) {
      // do stuff
      this.writeInstance();
    }
  }
  
  public void bad_nonfinalFieldProtectedWrittenTo_finalParameter_protected(final TestInstanceFieldRefs p) {
    synchronized (p) {
      /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
       * synchronized block; the object expression is final: final parameter "p"
       */
      // GOOD: protected field reference
      synchronized (p.protectedField) {
        // do stuff
        p.writeInstance();
      }
    }
  }
  
  public void bad_nonfinalFieldProtectedWrittenTo_effectivelyFinalParameter_protected(TestInstanceFieldRefs p) {
    synchronized (p) {
      /* NON-FINAL: field is non-final and protected but is WRITTEN TO in the
       * synchronized block; the object expression is final:  parameter "p"
       * is effectively final because it is not modified inside of the synchronized
       * block.
       */
      // GOOD: protected field reference
      synchronized (p.protectedField) {
        // do stuff
        p.writeInstance();
      }
    }
  }
  
  
  
  @Reads("this:Instance")
  private void readInstance() {
    // do stuff
  }

  @Writes("this:Instance")
  private void writeInstance() {
    // do stuff
  }
}
