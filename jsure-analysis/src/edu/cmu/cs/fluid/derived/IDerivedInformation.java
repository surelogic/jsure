/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/derived/IDerivedInformation.java,v 1.2 2007/07/10 22:16:35 aarong Exp $*/
package edu.cmu.cs.fluid.derived;

import edu.cmu.cs.fluid.*;

public interface IDerivedInformation {
  /**
   * Discard all knowledge of derivations performed.
   * This method waits until all threads stop performing derivations.
   * In the worst case, this method will ``starve.'' Since this method
   * is not actually useful unless people aren't deriving information frequently,
   * then there is no urgency to fix this starvation problem.
   * @throws DerivationException if this thread is currently deriving
   */
  void clear();

  /**
   * Determine whether we are in the process of deriving something.
   * In this case, we can't start another demand evaluation
   * or another production.
   * @return true if already in the process of deriving things.
   */
  boolean isDeriving();
  
  /** Ensure that the information is available.
   * @throws UnavailableException if not derived and cannot be demanded
   * @throws DerivationException if not derived and already in the process of deriving a different version.
   */
  void ensureDerived() throws UnavailableException;
  
  /**
   * Exception to be thrown when derived information is not available.
   */
  public static class UnavailableException extends FluidRuntimeException {
    public UnavailableException() {
      super();
    }
    public UnavailableException(String s) {
      super(s);
    }
  }
  
  /**
   * Exception thrown when derivation cannot take place.
   * For instance if a single thread tries to do two derivations at the same time.
   * @author boyland
   */
  public static class DerivationException extends FluidRuntimeException {
   public DerivationException() {
      super();
    }
    public DerivationException(String s) {
      super(s);
    }
  }  
}
