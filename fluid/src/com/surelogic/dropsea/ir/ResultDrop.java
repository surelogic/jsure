package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONSISTENT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TIMEOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VOUCHED;

import com.surelogic.InRegion;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IResultDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A code/model consistency result drop recording an analysis result in terms of
 * what promises are (partially or wholly) established in terms of a (possibly
 * empty) set of prerequisite assertion promises.
 * <p>
 * Not intended to be subclassed.
 */
public final class ResultDrop extends AnalysisResultDrop implements IResultDrop {

  /**
   * Constructs a new analysis result.
   */
  public ResultDrop(IRNode node) {
    super(node);
  }

  /**
   * Flags if this result indicates consistency with code.
   */
  @InRegion("DropState")
  private boolean consistent = false;

  public boolean isConsistent() {
    synchronized (f_seaLock) {
      return consistent;
    }
  }

  /**
   * Sets this result to indicate model/code consistency.
   */
  public void setConsistent() {
    synchronized (f_seaLock) {
      consistent = true;
    }
  }

  /**
   * Sets this result to indicate model/code inconsistency.
   */
  public void setInconsistent() {
    synchronized (f_seaLock) {
      consistent = false;
    }
  }

  /**
   * Sets this result to indicate model/code inconsistency.
   * 
   * @param value
   *          the consistency setting.
   */
  public void setConsistent(final boolean value) {
    synchronized (f_seaLock) {
      consistent = value;
    }
  }

  /**
   * Flags if this result drop was "vouched" for by a programmer even though it
   * is inconsistent.
   */
  @InRegion("DropState")
  private boolean vouched = false;

  public boolean isVouched() {
    synchronized (f_seaLock) {
      return vouched;
    }
  }

  /**
   * Sets this result as being "vouched" for by a programmer even though it is
   * inconsistent.
   */
  public void setVouched() {
    synchronized (f_seaLock) {
      vouched = true;
    }
  }

  /**
   * Flags if this result drop is inconsistent because the analysis timed out.
   */
  @InRegion("DropState")
  private boolean timeout = false;

  /**
   * Sets this analysis result to inconsistent and marks that this is because
   * its verifying analysis timed out.
   */
  public void setTimeout() {
    synchronized (f_seaLock) {
      setInconsistent();
      timeout = true;
    }
  }

  public boolean isTimeout() {
    synchronized (f_seaLock) {
      return timeout;
    }
  }

  /*
   * Consistency proof methods
   */

  @Override
  @RequiresLock("SeaLock")
  void proofInitialize() {
    super.proofInitialize();

    f_provedConsistent = isConsistent() || isVouched();
  }

  @Override
  @RequiresLock("SeaLock")
  boolean proofTransfer() {
    return proofTransferHelper(getTrusted());
  }

  @Override
  @RequiresLock("SeaLock")
  boolean proofTransferUsedByProofToTrustedResult(@NonNull AnalysisResultDrop trusted) {
    // if we are used by a proof and the trusted drop is a result
    // drop or result folder drop, then it is used by a proof
    if (f_usedByProof) {
      if (!trusted.f_usedByProof) {
        trusted.f_usedByProof = true;
        return true;
      }
    }
    return false;
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return RESULT_DROP;
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(VOUCHED, isVouched());
    s.addAttribute(CONSISTENT, isConsistent());
    s.addAttribute(TIMEOUT, isTimeout());
  }
}