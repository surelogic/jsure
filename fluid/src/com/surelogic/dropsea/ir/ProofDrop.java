package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_SRC;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROOF_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.RequiresLock;
import com.surelogic.common.i18n.AnalysisResultMessage;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.TypeUtil;

/**
 * Represents a promise or a result used in the code/model consistency proof.
 * 
 * @subtypedBy edu.cmu.cs.fluid.sea.PromiseDrop, edu.cmu.cs.fluid.sea.ResultDrop
 */
public abstract class ProofDrop extends IRReferenceDrop implements IProofDrop {

  protected ProofDrop(IRNode node) {
    super(node);
  }

  /**
   * Records if this element is able to be proved consistent with regards to the
   * whole-program.
   */
  @InRegion("DropState")
  private boolean provedConsistent = false;

  /**
   * Returns if this element has been judged to be consistent by
   * {@link Sea#updateConsistencyProof()}.
   * 
   * @return {@code true} if consistent, {@code false} otherwise (consistency
   *         can't be proved).
   */
  public boolean provedConsistent() {
    synchronized (f_seaLock) {
      return provedConsistent;
    }
  }

  void setProvedConsistent(boolean value) {
    synchronized (f_seaLock) {
      provedConsistent = value;
    }
  }

  /**
   * Records whether this result depends on something from source code.
   */
  @InRegion("DropState")
  private boolean derivedFromSrc = false;

  /**
   * Checks is this result depends upon something from source code.
   * 
   * @return {@code true} if this result depends on something from source code,
   *         {@code false} otherwise.
   */
  public boolean derivedFromSrc() {
    synchronized (f_seaLock) {
      return derivedFromSrc;
    }
  }

  void setDerivedFromSrc(boolean value) {
    synchronized (f_seaLock) {
      derivedFromSrc = value;
    }
  }

  /**
   * Records if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   */
  @InRegion("DropState")
  private boolean proofUsesRedDot = true;

  /**
   * Returns if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   * 
   * @return<code>true</code> if red dot, <code>false</code> if no red dot.
   */
  public boolean proofUsesRedDot() {
    synchronized (f_seaLock) {
      return proofUsesRedDot;
    }
  }

  void setProofUsesRedDot(boolean value) {
    synchronized (f_seaLock) {
      proofUsesRedDot = value;
    }
  }

  public boolean isFromSrc() {
    final IRNode n = getNode();
    if (n != null) {
      return !TypeUtil.isBinary(n);
    }
    return false;
  }

  /**
   * Returns a copy of set of result drops which directly trust (as an "and" or
   * an "or" precondition) this proof drop.
   * 
   * @return a set of result drops which trust this proof drop
   */
  @NonNull
  public final Set<AnalysisResultDrop> getTrustedBy() {
    final HashSet<AnalysisResultDrop> result = new HashSet<AnalysisResultDrop>();
    /*
     * check if any dependent result drop trusts this drop ("checks" doesn't
     * count)
     */
    synchronized (f_seaLock) {
      final List<AnalysisResultDrop> s = Sea.filterDropsOfType(AnalysisResultDrop.class, getDependentsReference());
      for (AnalysisResultDrop rd : s) {
        if (rd.getTrusted().contains(this)) {
          result.add(rd);
        }
      }
    }
    return result;
  }

  @InRegion("DropState")
  @Nullable
  private AnalysisResultMessage f_messageConsistent;

  public final void setMessageWhenProvedConsistent(int number, Object... args) {
    if (number < 1) {
      LOG.warning(I18N.err(247, number));
      return;
    }
    synchronized (f_seaLock) {
      JavaSourceReference srcRef = createSourceRef();
      f_messageConsistent = AnalysisResultMessage.getInstance(srcRef, number, args);
    }
  }

  @InRegion("DropState")
  @Nullable
  private AnalysisResultMessage f_messageInconsistent;

  public final void setMessageWhenNotProvedConsistent(int number, Object... args) {
    if (number < 1) {
      LOG.warning(I18N.err(247, number));
      return;
    }
    synchronized (f_seaLock) {
      JavaSourceReference srcRef = createSourceRef();
      f_messageInconsistent = AnalysisResultMessage.getInstance(srcRef, number, args);
    }
  }

  public final void setMessagesByJudgement(int whenConsistent, int whenInconsistent, Object... args) {
    synchronized (f_seaLock) {
      setMessageWhenProvedConsistent(whenConsistent, args);
      setMessageWhenNotProvedConsistent(whenInconsistent, args);
    }
  }

  /*
   * Consistency proof methods
   */

  /**
   * Called by {@link Sea#updateConsistencyProof()} to allow this proof drop to
   * initialize its state for running the the reverse flow analysis used by that
   * method to calculate promise consistency.
   */
  @RequiresLock("SeaLock")
  abstract protected void proofInitialize();

  /**
   * Called by {@link Sea#updateConsistencyProof()} on iteration to a
   * fixed-point to allow this proof drop to examine all proof drops with a
   * directed edge (in the drop-sea graph&mdash;see Halloran's thesis) to this
   * proof drop.
   */
  @RequiresLock("SeaLock")
  abstract protected void proofTransfer();

  /**
   * Called by {@link Sea#updateConsistencyProof()} when this proof drop's
   * consistency state has been changed by a call to {@link #proofTransfer()} on
   * iteration to a fixed-point to allow a conservative set of proof drops that
   * need to be examined on the next iteration to be added to the alorithm's
   * worklist.
   * <p>
   * Each proof drop that has changed should add all proof drops with a directed
   * edge (in the drop-sea graph&mdash;see Halloran's thesis) from the proof
   * drop that changed to them on the worklist.
   * 
   * @param mutableWorklist
   *          the worklist to add to.
   */
  @RequiresLock("SeaLock")
  abstract protected void proofAddToWorklistOnChange(Collection<ProofDrop> mutableWorklist);

  /**
   * Called by {@link Sea#updateConsistencyProof()} on each proof drop after the
   * consistency proof has been completed. This allows the drop to examine the
   * results and make any state changes necessary.
   * <p>
   * The default implementation changes the message based upon the analysis
   * judgment (if necessary) so overriding methods must invoke this one.
   */
  @RequiresLock("SeaLock")
  @MustInvokeOnOverride
  protected void proofFinalize() {
    if (provedConsistent()) {
      if (f_messageConsistent != null)
        setMessage(f_messageConsistent);
    } else {
      if (f_messageInconsistent != null)
        setMessage(f_messageInconsistent);
    }
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return PROOF_DROP;
  }

  @Override
  @MustInvokeOnOverride
  public void preprocessRefs(SeaSnapshot s) {
    super.preprocessRefs(s);
    for (IAnalysisHintDrop c : getAnalysisHintsAbout()) {
      if (c instanceof Drop)
        s.snapshotDrop((Drop) c);
    }
  }

  @Override
  @MustInvokeOnOverride
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(USES_RED_DOT_ATTR, proofUsesRedDot());
    s.addAttribute(PROVED_ATTR, provedConsistent());
    s.addAttribute(DERIVED_FROM_SRC_ATTR, derivedFromSrc());
    s.addAttribute(FROM_SRC, isFromSrc());
  }
}