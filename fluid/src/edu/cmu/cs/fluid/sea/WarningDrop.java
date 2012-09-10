package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting warnings that a particular verifying analyis wants to
 * bring to the attention of the tool user.
 * <p>
 * This type is <b>not</b> used for scrubber warnings&mdash;for that purpose use
 * {@link PromiseWarningDrop}.
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