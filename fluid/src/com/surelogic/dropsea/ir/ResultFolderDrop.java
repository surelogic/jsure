package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.FOLDER_LOGIC_OPERATOR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT_FOLDER_DROP;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * 
 * A code/model consistency result drop grouping a set of analysis result in
 * terms of what promises are (partially or wholly) established in terms of
 * those results.
 * <p>
 * Not intended to be subclassed.
 */
public final class ResultFolderDrop extends AnalysisResultDrop implements IResultFolderDrop {

  /**
   * Constructs a new <b>And</b> analysis result folder pointing to the passed
   * node. Results placed in this folder are conjoined (
   * {@link LogicOperator#AND}) in the model-code consistency proof.
   * 
   * @param node
   *          referenced by the folder.
   * @return a folder.
   */
  public static ResultFolderDrop newAndFolder(IRNode node) {
    return new ResultFolderDrop(node, LogicOperator.AND);
  }

  /**
   * Constructs a new <b>Or</b> analysis result folder pointing to the passed
   * node. Results placed in this folder are disjoined ({@link LogicOperator#OR}
   * ) in the model-code consistency proof.
   * 
   * @param node
   *          referenced in the warning
   * @return a warning.
   */
  public static ResultFolderDrop newOrFolder(IRNode node) {
    return new ResultFolderDrop(node, LogicOperator.OR);
  }

  @NonNull
  private final LogicOperator f_operator;

  /**
   * Constructs a new analysis result folder.
   */
  private ResultFolderDrop(IRNode node, LogicOperator operator) {
    super(node);
    f_operator = operator == null ? LogicOperator.AND : operator;
  }

  @NonNull
  public LogicOperator getLogicOperator() {
    return f_operator;
  }

  /*
   * Consistency proof methods
   */

  @Override
  @RequiresLock("SeaLock")
  protected void proofInitialize() {
    super.proofInitialize();

    setProvedConsistent(true);
  }

  @Override
  @RequiresLock("SeaLock")
  protected void proofTransfer() {
    if (getLogicOperator() == LogicOperator.AND) {
      /*
       * CONJUNCTION (AND)
       */
      for (ProofDrop result : getTrusted()) {
        // all must be consistent for this folder to be consistent
        setProvedConsistent(provedConsistent() & result.provedConsistent());
        // any red dot means this folder depends upon a red dot
        if (result.proofUsesRedDot())
          setProofUsesRedDot(true);
        // push along if derived from source code
        setDerivedFromSrc(derivedFromSrc() | result.derivedFromSrc());
        // push along if derived from a warning hint
        setDerivedFromWarningHint(derivedFromWarningHint() | result.derivedFromWarningHint());
      }
    } else {
      /*
       * DISJUNCTION (OR)
       */

      // Our lattice -- lower index is better
      // {consistency judgment, uses red dot, has warning hint}
      boolean[][] judgmentReddotWarningLattice = { { true, false, false }, { true, false, true }, { true, true, false },
          { true, true, true }, { false, true, false }, { false, true, true }, { false, false, true }, { false, false, false }, };
      int bestIndex = judgmentReddotWarningLattice.length - 1;

      boolean overall_or_derivedFromSource = false;

      for (ProofDrop result : getTrusted()) {
        for (int i = 0; i < judgmentReddotWarningLattice.length; i++) {
          final boolean[] lattice = judgmentReddotWarningLattice[i];
          if (lattice[0] = result.provedConsistent() && lattice[1] == result.proofUsesRedDot()
              && lattice[2] == result.derivedFromWarningHint()) {
            if (i < bestIndex)
              bestIndex = i;
          }
        }

        // if anything is derived from source we will be as well
        overall_or_derivedFromSource |= result.derivedFromSrc();
      }

      setProvedConsistent(judgmentReddotWarningLattice[bestIndex][0]);
      setProofUsesRedDot(judgmentReddotWarningLattice[bestIndex][1]);
      setDerivedFromWarningHint(judgmentReddotWarningLattice[bestIndex][2]);
      setDerivedFromSrc(derivedFromSrc() | overall_or_derivedFromSource);
    }
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return RESULT_FOLDER_DROP;
  }

  @Override
  @MustInvokeOnOverride
  public void snapshotAttrs(Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(FOLDER_LOGIC_OPERATOR, getLogicOperator().toString());
  }
}
