package edu.cmu.cs.fluid.sea;

/**
 * An implementation of a {@link SeaConsistencyProofHook} that provides empty
 * implementations of all methods. This class is useful if only one operation
 * needs to be overridden.
 */
public abstract class AbstractSeaConsistencyProofHook implements SeaConsistencyProofHook {

  @Override
  public void preConsistencyProof(Sea sea) {
    // by default do nothing
  }

  @Override
  public void postConsistencyProof(Sea sea) {
    // by default do nothing
  }
}
