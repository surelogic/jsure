package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FOLDER_LOGIC_OPERATOR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.RESULT_FOLDER_DROP;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IProofDrop;
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

  public final DropType getDropType() {
	return DropType.RESULT_FOLDER;
  }
  
  @Override
  @NonNull
  public LogicOperator getLogicOperator() {
    return f_operator;
  }

  @Override
  boolean immediatelyConsistent() {
    boolean result = true; // assume the best
    synchronized (f_seaLock) {
      if (f_operator == LogicOperator.AND) {
        for (IProofDrop drop : getTrusted()) {
          if (drop instanceof ResultDrop) {
            result &= ((ResultDrop) drop).isConsistent();
          } else {
            result &= drop.provedConsistent();
          }
        }
      } else if (f_operator == LogicOperator.OR) {
        result = provedConsistent();
        if (f_choiceOfOrFolder instanceof ResultDrop) {
          result = ((ResultDrop) f_choiceOfOrFolder).isConsistent();
        }
      }
    }
    return result;
  }

  /*
   * Consistency proof methods
   */

  @Override
  @RequiresLock("SeaLock")
  void proofInitialize() {
    super.proofInitialize();

    f_provedConsistent = true;
  }

  @InRegion("DropState")
  ProofDrop f_choiceOfOrFolder = null;

  @Override
  @RequiresLock("SeaLock")
  boolean proofTransfer() {

    if (getLogicOperator() == LogicOperator.AND) {
      /*
       * CONJUNCTION (AND)
       */
      return proofTransferHelper(getTrusted());

    } else {
      /*
       * DISJUNCTION (OR)
       */

      // Our lattice -- lower index is better
      // {consistency, red-dot, immediate, warning-hint}
      boolean[][] judgmentReddotWarningLattice = {

      { true, false, true, false },

      { true, false, false, false },

      { true, false, true, true },

      { true, false, false, true },

      { true, true, true, false },

      { true, true, false, false },

      { true, true, true, true },

      { true, true, false, true },

      { false, true, true, false },

      { false, true, true, true },

      { false, false, true, false },

      { false, false, true, true },

      { false, true, false, false },

      { false, true, false, true },

      { false, false, false, false },

      { false, false, false, true },

      };

      int indexOfBestChoice = Integer.MAX_VALUE;

      ProofDrop chosenDrop = null;
      for (ProofDrop choice : getTrusted()) {
        for (int i = 0; i < judgmentReddotWarningLattice.length; i++) {
          final boolean[] lattice = judgmentReddotWarningLattice[i];
          if (lattice[0] == choice.provedConsistent() && lattice[1] == choice.proofUsesRedDot()
              && lattice[2] == choice.immediatelyConsistent() && lattice[3] == choice.derivedFromWarningHint()) {
            if (i < indexOfBestChoice) {
              indexOfBestChoice = i;
              chosenDrop = choice;
            }
          }
        }
      }

      if (chosenDrop != null) {
        f_choiceOfOrFolder = chosenDrop;
        return proofTransferDropHelper(chosenDrop);
      } else
        return false;
    }
  }

  @Override
  @RequiresLock("SeaLock")
  boolean proofTransferUsedByProofToTrustedResult(@NonNull AnalysisResultDrop trusted) {
    // if we are used by a proof and the trusted drop is a result drop or result
    // folder drop, then it is used by a proof
    if (f_usedByProof) {
      if (getLogicOperator() == LogicOperator.AND) {
        if (!trusted.f_usedByProof) {
          trusted.f_usedByProof = true;
          return true;
        }
      } else {
        // for OR check that the drop is our choice, proof only must use the
        // choice
        if (trusted == f_choiceOfOrFolder && !trusted.f_usedByProof) {
          trusted.f_usedByProof = true;
          return true;
        }
      }
    }
    return false;
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
  @RequiresLock("SeaLock")
  public void snapshotAttrs(XmlCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(FOLDER_LOGIC_OPERATOR, getLogicOperator().toString());
  }
}
