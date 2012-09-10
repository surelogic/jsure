package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 * <p>
 * The only subtype of this should be {@link WarningDrop}. This type is not
 * intended to be otherwise subtyped.
 */
public class InfoDrop extends IRReferenceDrop implements IReportedByAnalysisDrop {

  public interface Factory {
    InfoDrop create();
  }

  public static final Factory factory = new Factory() {
    public InfoDrop create() {
      return new InfoDrop();
    }
  };
}