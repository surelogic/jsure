package edu.cmu.cs.fluid.sea.proxy;

import com.surelogic.analysis.IIRAnalysis;

import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.InfoDrop.Factory;

/**
 * Temporary builder to help analyses be parallelized for both {@link InfoDrop}
 * and {@link WarningDrop} instances.
 */
public class InfoDropBuilder extends AbstractDropBuilder {

  private final Factory factory;

  private InfoDropBuilder(Factory f) {
    factory = f;
  }

  public static InfoDropBuilder create(IIRAnalysis a, Factory f) {
    InfoDropBuilder rv = new InfoDropBuilder(f);
    a.handleBuilder(rv);
    return rv;
  }

  @Override
  public int build() {
    if (!isValid()) {
      return 0;
    }
    InfoDrop rd = factory.create();
    return buildDrop(rd);
  }
}
