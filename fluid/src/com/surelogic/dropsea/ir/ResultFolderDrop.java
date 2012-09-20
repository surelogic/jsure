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
   * node. Results placed in this folder are conjoined ({@link FolderLogic#AND})
   * in the model-code consistency proof.
   * 
   * @param node
   *          referenced by the folder.
   * @return a folder.
   */
  public static ResultFolderDrop newAndFolder(IRNode node) {
    return new ResultFolderDrop(node, FolderLogic.AND);
  }

  /**
   * Constructs a new <b>Or</b> analysis result folder pointing to the passed
   * node. Results placed in this folder are disjoined ({@link FolderLogic#OR})
   * in the model-code consistency proof.
   * 
   * @param node
   *          referenced in the warning
   * @return a warning.
   */
  public static ResultFolderDrop newOrFolder(IRNode node) {
    return new ResultFolderDrop(node, FolderLogic.OR);
  }

  @NonNull
  private final FolderLogic f_operator;

  /**
   * Constructs a new analysis result folder.
   */
  private ResultFolderDrop(IRNode node, FolderLogic operator) {
    super(node);
    f_operator = operator == null ? FolderLogic.AND : operator;
  }

  @NonNull
  public FolderLogic getFolderLogic() {
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
    if (getFolderLogic() == FolderLogic.AND) {
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
      }
    } else {
      /*
       * DISJUNCTION (OR)
       */
      boolean overall_or_Result = false;
      boolean overall_or_UsesRedDot = false;
      boolean overall_or_derivedFromSource = false;

      for (ProofDrop result : getTrusted()) {
        boolean choiceResult = result.provedConsistent();
        boolean choiceUsesRedDot = result.proofUsesRedDot();
        // if anything is derived from source we will be as well
        overall_or_derivedFromSource |= result.derivedFromSrc();

        // should we choose this choice? Our lattice is:
        // o consistent
        // o consistent/red dot
        // o inconsistent/red dot
        // o inconsistent
        // so we want to pick the "highest" result
        // ignore bottom of lattice, this was our default (set above)
        if (choiceResult) {
          if (!choiceUsesRedDot) {
            // best possible outcome
            overall_or_Result = choiceResult;
            overall_or_UsesRedDot = choiceUsesRedDot;
          } else {
            if (!overall_or_Result) {
              // take it, since so far we think we are inconsistent
              overall_or_Result = choiceResult;
              overall_or_UsesRedDot = choiceUsesRedDot;
            }
          }
        } else {
          if (!choiceUsesRedDot) {
            if (!overall_or_Result) {
              // take it, since so far we might be sure we are wrong
              overall_or_Result = choiceResult;
              overall_or_UsesRedDot = choiceUsesRedDot;
            }
          }
        }
      }
      setProvedConsistent(overall_or_Result);
      setProofUsesRedDot(overall_or_UsesRedDot);
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
    s.addAttribute(FOLDER_LOGIC_OPERATOR, getFolderLogic().toString());
  }
}
