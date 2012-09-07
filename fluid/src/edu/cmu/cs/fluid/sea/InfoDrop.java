package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 */
public class InfoDrop extends IRReferenceDrop implements IAnalysisResultDrop {

  public InfoDrop() {
  }

  public interface Factory {
    InfoDrop create();
  }

  public static final Factory factory = new Factory() {
    public InfoDrop create() {
      return new InfoDrop();
    }
  };
}