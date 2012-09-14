package com.surelogic.dropsea.ir;

import java.util.List;

import com.surelogic.RequiresLock;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * 
 * A code/model consistency result drop grouping a set of analysis result in
 * terms of what promises are (partially or wholly) established in terms of
 * those results.
 * <p>
 * Not intended to be subclassed.
 */
public final class ResultFolderDrop extends AnalysisResultDrop implements IResultFolderDrop {

  /*
   * XML attribute constants
   */
  public static final String SUB_FOLDER = "sub-folder";
  public static final String RESULT = "result";

  /**
   * Constructs a new analysis result folder.
   */
  public ResultFolderDrop(IRNode node) {
    super(node);
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
      this.addDependent(result);
    }
  }

  /**
   * Gets all the analysis results directly within this folder. If sub-folders
   * exist, analysis results within the sub-folders are <b>not</b> returned.
   * 
   * @return a non-null (possibly empty) set of analysis results.
   */
  public List<ResultDrop> getAnalysisResults() {
    final List<ResultDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(ResultDrop.class, getDependentsReference());
    }
    return result;
  }

  /**
   * Gets all the sub-folders within this folder.
   * 
   * @return a non-null (possibly empty) set of analysis result folders.
   */
  public List<ResultFolderDrop> getSubFolders() {
    final List<ResultFolderDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(ResultFolderDrop.class, getDependentsReference());
    }
    return result;
  }

  /**
   * Gets all the analysis results and sub-folders within this folder.
   * 
   * @return a non-null (possibly empty) set of analysis results and
   *         sub-folders.
   */
  public List<AnalysisResultDrop> getContents() {
    final List<AnalysisResultDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(AnalysisResultDrop.class, getDependentsReference());
    }
    return result;
  }

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  @RequiresLock("SeaLock")
  void proofInitialize() {
    super.proofInitialize();

    setProvedConsistent(true);
  }

  @Override
  @RequiresLock("SeaLock")
  void proofTransfer() {
    for (AnalysisResultDrop result : getContents()) {
      // all must be consistent for this folder to be consistent
      setProvedConsistent(provedConsistent() & result.provedConsistent());
      // any red dot means this folder depends upon a red dot
      if (result.proofUsesRedDot())
        setProofUsesRedDot(true);
      // push along if derived from source code
      setDerivedFromSrc(derivedFromSrc() | result.derivedFromSrc());
    }
  }

  @Override
  public String getXMLElementName() {
    return AbstractXMLReader.RESULT_FOLDER_DROP;
  }

  @Override
  public void preprocessRefs(SeaSnapshot s) {
    super.preprocessRefs(s);
    for (Drop t : getSubFolders()) {
      s.snapshotDrop(t);
    }
    for (Drop t : getAnalysisResults()) {
      s.snapshotDrop(t);
    }
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    for (Drop t : getSubFolders()) {
      s.refDrop(db, SUB_FOLDER, t);
    }
    for (Drop t : getAnalysisResults()) {
      s.refDrop(db, RESULT, t);
    }
  }
}
