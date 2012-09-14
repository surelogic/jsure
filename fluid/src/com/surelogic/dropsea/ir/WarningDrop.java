package com.surelogic.dropsea.ir;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for reporting warnings that a particular verifying analyis wants to
 * bring to the attention of the tool user.
 * <p>
 * This type is <b>not</b> used for scrubber warnings&mdash;for that purpose use
 * {@link PromiseWarningDrop}.
 */
public final class WarningDrop extends InfoDrop {
  public WarningDrop(IRNode node) {
    super(node);
  }
}