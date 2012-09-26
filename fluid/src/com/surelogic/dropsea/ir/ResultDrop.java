package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONSISTENT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TIMEOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VOUCHED;

import com.surelogic.InRegion;
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
  protected void proofInitialize() {
    super.proofInitialize();

    setProvedConsistent(isConsistent() || isVouched());
  }

  @Override
  @RequiresLock("SeaLock")
  protected void proofTransfer() {
    // "and" trusted proof drops
    for (final ProofDrop proofDrop : getTrusted()) {
      // all must be consistent for this drop to be consistent
      setProvedConsistent(provedConsistent() & proofDrop.provedConsistent());
      // any red dot means this drop depends upon a red dot
      if (proofDrop.proofUsesRedDot())
        setProofUsesRedDot(true);
      // push along if derived from source code
      setDerivedFromSrc(derivedFromSrc() | proofDrop.derivedFromSrc());
      // push along if derived from a warning hint
      setDerivedFromWarningHint(derivedFromWarningHint() | proofDrop.derivedFromWarningHint());
    }
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