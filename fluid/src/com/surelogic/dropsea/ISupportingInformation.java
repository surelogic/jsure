package com.surelogic.dropsea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * The interface for the base class for all supporting information within the
 * sea, intended to allow multiple implementations. The analysis uses the IR
 * drop-sea and the Eclipse client loads snapshots using a IR-free drop-sea.
 */
public interface ISupportingInformation {
  /**
   * @return the fAST location this supporting information references, can be
   *         <code>null</code>
   */
  public IRNode getLocation();

  /**
   * @return a message describing the point of this supporting information
   */
  public String getMessage();

  /**
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  public ISrcRef getSrcRef();

  public boolean sameAs(IRNode link, int num, Object[] args);

  public boolean sameAs(IRNode link, String message);
}
