package com.surelogic.dropsea.ir;

/**
 * An interface to inject code before and/or after the consistency proof is run
 * on every call to {@link Sea#updateConsistencyProof()}.
 * <p>
 * Instances are registered via
 * {@link Sea#addConsistencyProofHook(SeaConsistencyProofHook)} and removed via
 * .
 */
public interface SeaConsistencyProofHook {

  /**
   * Called prior to the consistency proof being executed on every call to
   * {@link Sea#updateConsistencyProof()}.
   * <p>
   * <b>Warning:</b> this method is called holding {@link Sea#getSeaLock()}.
   * 
   * @param sea
   *          a sea.
   */
  void preConsistencyProof(Sea sea);

  /**
   * Called after the consistency proof has been executed on every call to
   * {@link Sea#updateConsistencyProof()}.
   * <p>
   * <b>Warning:</b> this method is called holding {@link Sea#getSeaLock()}.
   * 
   * @param sea
   *          a sea.
   */
  void postConsistencyProof(Sea sea);
}
