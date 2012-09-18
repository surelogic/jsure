package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT_FOLDER_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.SUB_FOLDER;

import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
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

  @NonNull
  public List<ResultDrop> getAnalysisResults() {
    final List<ResultDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(ResultDrop.class, getDependentsReference());
    }
    return result;
  }

  @NonNull
  public List<ResultFolderDrop> getSubFolders() {
    final List<ResultFolderDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(ResultFolderDrop.class, getDependentsReference());
    }
    return result;
  }

  @NonNull
  public List<AnalysisResultDrop> getContents() {
    final List<AnalysisResultDrop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsOfType(AnalysisResultDrop.class, getDependentsReference());
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
