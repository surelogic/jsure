package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_WARNING_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_SRC;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROOF_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IHintDrop;
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
  private boolean f_provedConsistent = false;

  /**
   * Returns if this element has been judged to be consistent by
   * {@link Sea#updateConsistencyProof()}.
   * 
   * @return {@code true} if consistent, {@code false} otherwise (consistency
   *         can't be proved).
   */
  public boolean provedConsistent() {
    synchronized (f_seaLock) {
      return f_provedConsistent;
    }
  }

  void setProvedConsistent(boolean value) {
    synchronized (f_seaLock) {
      f_provedConsistent = value;
    }
  }

  /**
   * Records whether this result depends on something from source code.
   */
  @InRegion("DropState")
  private boolean f_derivedFromSrc = false;

  public boolean derivedFromSrc() {
    synchronized (f_seaLock) {
      return f_derivedFromSrc;
    }
  }

  void setDerivedFromSrc(boolean value) {
    synchronized (f_seaLock) {
      f_derivedFromSrc = value;
    }
  }

  /**
   * Records whether this result depends upon something with a warning hint
   * about it.
   */
  @InRegion("DropState")
  private boolean f_derivedFromWarningHint = false;

  public boolean derivedFromWarningHint() {
    synchronized (f_seaLock) {
      return f_derivedFromWarningHint;
    }
  }

  void setDerivedFromWarningHint(boolean value) {
    synchronized (f_seaLock) {
      f_derivedFromWarningHint = value;
    }
  }

  /**
   * Records if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   */
  @InRegion("DropState")
  private boolean f_proofUsesRedDot = true;

  public boolean proofUsesRedDot() {
    synchronized (f_seaLock) {
      return f_proofUsesRedDot;
    }
  }

  void setProofUsesRedDot(boolean value) {
    synchronized (f_seaLock) {
      f_proofUsesRedDot = value;
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
  private String f_messageConsistent;
  @InRegion("DropState")
  @Nullable
  private String f_messageConsistentCanonical;

  public final void setMessageWhenProvedConsistent(int number, Object... args) {
    if (number < 1) {
      LOG.warning(I18N.err(257, number));
      return;
    }
    synchronized (f_seaLock) {
      f_messageConsistent = args.length == 0 ? I18N.res(number) : I18N.res(number, args);
      f_messageConsistentCanonical = args.length == 0 ? I18N.resc(number) : I18N.resc(number, args);
    }
  }

  @InRegion("DropState")
  @Nullable
  private String f_messageInconsistent;
  @InRegion("DropState")
  @Nullable
  private String f_messageInconsistentCanonical;

  public final void setMessageWhenNotProvedConsistent(int number, Object... args) {
    if (number < 1) {
      LOG.warning(I18N.err(257, number));
      return;
    }
    synchronized (f_seaLock) {
      f_messageInconsistent = args.length == 0 ? I18N.res(number) : I18N.res(number, args);
      f_messageInconsistentCanonical = args.length == 0 ? I18N.resc(number) : I18N.resc(number, args);
    }
  }

  public final void setMessagesByJudgement(int whenConsistent, int whenInconsistent, Object... args) {
    synchronized (f_seaLock) {
      setMessageWhenProvedConsistent(whenConsistent, args);
      setMessageWhenNotProvedConsistent(whenInconsistent, args);
    }
  }

  /**
   * Holds the set of promises proposed by this drop if not proved consistent.
   */
  @InRegion("DropState")
  @UniqueInRegion("DropState")
  private List<ProposedPromiseDrop> f_proposalsNotProvedConsistent = null;

  @Override
  @RequiresLock("SeaLock")
  @NonNull
  protected List<ProposedPromiseDrop> getConditionalProposals() {
    if (!provedConsistent()) {
      if (f_proposalsNotProvedConsistent != null)
        return f_proposalsNotProvedConsistent;
    }
    return Collections.emptyList();
  }

  /**
   * Adds a proposed promise to this drop that will only be proposed to the tool
   * user when this drop is not proved consistent.
   * 
   * @param proposal
   *          the proposed promise.
   */
  public final void addProposalNotProvedConsistent(ProposedPromiseDrop proposal) {
    if (proposal != null) {
      synchronized (f_seaLock) {
        if (f_proposalsNotProvedConsistent == null) {
          f_proposalsNotProvedConsistent = new ArrayList<ProposedPromiseDrop>(1);
        }
        f_proposalsNotProvedConsistent.add(proposal);
      }
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
        setMessageHelper(f_messageConsistent, f_messageConsistentCanonical);
    } else {
      if (f_messageInconsistent != null)
        setMessageHelper(f_messageInconsistent, f_messageInconsistentCanonical);
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
    for (IHintDrop c : getHints()) {
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
    s.addAttribute(DERIVED_FROM_WARNING_ATTR, derivedFromWarningHint());
    s.addAttribute(FROM_SRC, isFromSrc());
  }
}