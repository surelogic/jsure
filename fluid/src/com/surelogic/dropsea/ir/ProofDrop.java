package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DERIVED_FROM_WARNING_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROOF_DROP;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROVED_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.USES_RED_DOT_ATTR;

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
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.ir.SeaSnapshot;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents a promise or a result used in the code/model consistency proof.
 * 
 * @subtypedBy edu.cmu.cs.fluid.sea.PromiseDrop, edu.cmu.cs.fluid.sea.ResultDrop
 */
public abstract class ProofDrop extends Drop implements IProofDrop {

  protected ProofDrop(IRNode node) {
    super(node);
  }

  /**
   * Records if this element is able to be proved consistent with regards to the
   * whole-program.
   */
  @InRegion("DropState")
  boolean f_provedConsistent = false;

  /**
   * Returns if this element has been judged to be consistent by
   * {@link Sea#updateConsistencyProof()}.
   * 
   * @return {@code true} if consistent, {@code false} otherwise (consistency
   *         can't be proved).
   */
  @Override
  public boolean provedConsistent() {
    synchronized (f_seaLock) {
      return f_provedConsistent;
    }
  }

  /**
   * Records whether this result depends on something from source code.
   */
  @InRegion("DropState")
  boolean f_derivedFromSrc = false;

  @Override
  public boolean derivedFromSrc() {
    synchronized (f_seaLock) {
      return f_derivedFromSrc;
    }
  }

  /**
   * Records whether this result depends upon something with a warning hint
   * about it.
   */
  @InRegion("DropState")
  boolean f_derivedFromWarningHint = false;

  @Override
  public boolean derivedFromWarningHint() {
    synchronized (f_seaLock) {
      return f_derivedFromWarningHint;
    }
  }

  /**
   * Records if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   */
  @InRegion("DropState")
  boolean f_proofUsesRedDot = true;

  @Override
  public boolean proofUsesRedDot() {
    synchronized (f_seaLock) {
      return f_proofUsesRedDot;
    }
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
  protected String f_messageConsistent;
  @InRegion("DropState")
  @Nullable
  protected String f_messageConsistentCanonical;

  public final void setMessageWhenProvedConsistent(int number, Object... args) {
    if (number < 1) {
      SLLogger.getLogger().warning(I18N.err(257, number));
      return;
    }
    synchronized (f_seaLock) {
      f_messageConsistent = args.length == 0 ? I18N.res(number) : I18N.res(number, args);
      f_messageConsistentCanonical = args.length == 0 ? I18N.resc(number) : I18N.resc(number, args);
    }
  }

  @InRegion("DropState")
  @Nullable
  protected String f_messageInconsistent;
  @InRegion("DropState")
  @Nullable
  protected String f_messageInconsistentCanonical;

  public final void setMessageWhenNotProvedConsistent(int number, Object... args) {
    if (number < 1) {
      SLLogger.getLogger().warning(I18N.err(257, number));
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

  /**
   * Gets if this drop is immediately consistent. Immediately consistent means
   * the local analysis judgment rather than the overall consistency proof
   * judgment.
   * <p>
   * This method returns {@link #provedConsistent()} by default, but is intended
   * to be overridden by subtypes that can provide a more precise result.
   * 
   * @return {@code true} if this drop is immediately consistent, {@code false}
   *         if it is not.
   */
  boolean immediatelyConsistent() {
    return provedConsistent();
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
  abstract void proofInitialize();

  /**
   * Called by {@link Sea#updateConsistencyProof()} on iteration to a
   * fixed-point to allow this proof drop to examine all proof drops with a
   * directed edge (in the drop-sea graph&mdash;see Halloran's thesis) to this
   * proof drop.
   * 
   * @return {@code true} if something changes, {@code false} otherwise.
   */
  @RequiresLock("SeaLock")
  abstract boolean proofTransfer();

  /**
   * Transfers data from all the passed proof drops.
   * 
   * @param proofDrops
   *          a list of proof drops to transfer.
   * @return {@code true} if something changes, {@code false} otherwise.
   */
  @RequiresLock("SeaLock")
  final boolean proofTransferHelper(@NonNull final Collection<ProofDrop> proofDrops) {
    boolean changed = false; // assume the best
    for (final ProofDrop proofDrop : proofDrops) {
      changed |= proofTransferDropHelper(proofDrop);
    }
    return changed;
  }

  /**
   * Transfers data from a single proof drop to this drop.
   * 
   * @param proofDrop
   *          to transfer from.
   * @return {@code true} if something changes, {@code false} otherwise.
   */
  @RequiresLock("SeaLock")
  final boolean proofTransferDropHelper(final @NonNull ProofDrop proofDrop) {
    boolean changed = false; // assume the best

    // all must be consistent for this drop to be consistent
    if (f_provedConsistent && !proofDrop.f_provedConsistent) {
      f_provedConsistent = false;
      changed = true;
    }
    // any red dot means this drop depends upon a red dot
    if (!f_proofUsesRedDot && proofDrop.f_proofUsesRedDot) {
      f_proofUsesRedDot = true;
      changed = true;
    }
    // push along if derived from source code
    if (!f_derivedFromSrc && proofDrop.f_derivedFromSrc) {
      f_derivedFromSrc = true;
      changed = true;
    }
    // push along if derived from a warning hint
    if (!f_derivedFromWarningHint && proofDrop.f_derivedFromWarningHint) {
      f_derivedFromWarningHint = true;
      changed = true;
    }

    return changed;
  }

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
  void proofFinalize() {
    if (provedConsistent()) {
      if (f_messageConsistent != null)
        setMessageHelper(f_messageConsistent, f_messageConsistentCanonical);
      if (f_proposalsNotProvedConsistent != null) {
        for (ProposedPromiseDrop ppd : f_proposalsNotProvedConsistent) {
          ppd.invalidate();
        }
        f_proposalsNotProvedConsistent = null;
      }
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
  @RequiresLock("SeaLock")
  public void snapshotAttrs(XmlCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(USES_RED_DOT_ATTR, proofUsesRedDot());
    s.addAttribute(PROVED_ATTR, provedConsistent());
    s.addAttribute(DERIVED_FROM_SRC_ATTR, derivedFromSrc());
    s.addAttribute(DERIVED_FROM_WARNING_ATTR, derivedFromWarningHint());
  }
}