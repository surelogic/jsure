package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ENCLOSED_IN_FOLDER;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FOLDER_LOGIC_OPERATOR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT_FOLDER_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.SUB_FOLDER;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;

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

  /**
   * The set of promise drops being checked, or established, by this result.
   */
  @UniqueInRegion("DropState")
  private final Set<AnalysisResultDrop> f_contains = new HashSet<AnalysisResultDrop>();

  @NonNull
  private final FolderLogic f_operator;

  /**
   * Constructs a new analysis result folder.
   */
  private ResultFolderDrop(IRNode node, FolderLogic operator) {
    super(node);
    f_operator = operator == null ? FolderLogic.AND : operator;
  }

  /**
   * Adds an analysis result into this folder. The result added could possibly
   * be another folder&mdash;nesting of folders is allowed.
   * 
   * @param result
   *          an analysis result.
   */
  public void add(AnalysisResultDrop result) {
    if (result == null)
      return;
    synchronized (f_seaLock) {
      f_contains.add(result);
      this.addDependent(result);
    }
  }

  @NonNull
  public FolderLogic getFolderLogic() {
    return f_operator;
  }

  @NonNull
  public List<ResultDrop> getAnalysisResults() {
    final List<ResultDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(ResultDrop.class, f_contains);
    }
    return result;
  }

  @NonNull
  public List<ResultFolderDrop> getSubFolders() {
    final List<ResultFolderDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(ResultFolderDrop.class, f_contains);
    }
    return result;
  }

  @NonNull
  public List<AnalysisResultDrop> getContents() {
    final List<AnalysisResultDrop> result = new ArrayList<AnalysisResultDrop>();
    synchronized (f_seaLock) {
      result.addAll(f_contains);
    }
    return result;
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
      for (AnalysisResultDrop result : getContents()) {
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

      for (AnalysisResultDrop result : getContents()) {
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
      setDerivedFromSrc(overall_or_derivedFromSource);
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
  public void preprocessRefs(SeaSnapshot s) {
    super.preprocessRefs(s);
    for (Drop t : getContents()) {
      s.snapshotDrop(t);
    }
  }

  @Override
  @MustInvokeOnOverride
  public void snapshotAttrs(Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(ENCLOSED_IN_FOLDER, isInResultFolder());
    s.addAttribute(FOLDER_LOGIC_OPERATOR, getFolderLogic().toString());
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    for (Drop t : getContents()) {
      if (t instanceof ResultFolderDrop)
        s.refDrop(db, SUB_FOLDER, t);
      else
        s.refDrop(db, RESULT, t);
    }
  }
}
