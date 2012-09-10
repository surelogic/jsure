package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting warnings NOT for scrubber warnings -- use
 * PromiseWarningDrop instead
 */
public final class WarningDrop extends InfoDrop {
  public WarningDrop() {
    super();
  }

  public static final Factory factory = new Factory() {
    public InfoDrop create() {
      return new WarningDrop();
    }
  };
}