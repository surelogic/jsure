package edu.cmu.cs.fluid.sea;

import java.util.Set;

import com.surelogic.common.xml.XMLCreator.Builder;

import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * 
 * A code/model consistency result drop grouping a set of analysis result in
 * terms of what promises are (partially or wholly) established in terms of
 * those results.
 * <p>
 * Not intended to be subclassed.
 */
public final class ResultFolderDrop extends AbstractResultDrop {

  /*
   * XML attribute constants
   */
  public static final String SUB_FOLDER = "sub-folder";
  public static final String RESULT = "result";

  /**
   * Constructs a new analysis result folder.
   */
  public ResultFolderDrop() {
  }

  /**
   * Adds an analysis result into this folder. The result added could possibly
   * be another folder&mdash;nesting of folders is allowed.
   * 
   * @param result
   *          an analysis result.
   */
  public void add(AbstractResultDrop result) {
    if (result == null)
      return;

    this.addDependent(result);
  }

  /**
   * Gets all the analysis results directly within this folder. If sub-folders
   * exist, analysis results within the sub-folders are <b>not</b> returned.
   * 
   * @return a non-null (possibly empty) set of analysis results.
   */
  public Set<ResultDrop> getAnalysisResults() {
    final Set<ResultDrop> result = Sea.filterDropsOfType(ResultDrop.class, getDeponents());
    return result;
  }

  /**
   * Gets all the sub-folders within this folder.
   * 
   * @return a non-null (possibly empty) set of analysis result folders.
   */
  public Set<ResultFolderDrop> getSubFolders() {
    final Set<ResultFolderDrop> result = Sea.filterDropsOfType(ResultFolderDrop.class, getDeponents());
    return result;
  }

  /**
   * Gets all the analysis results and sub-folders within this folder.
   * 
   * @return a non-null (possibly empty) set of analysis results and
   *         sub-folders.
   */
  public Set<AbstractResultDrop> getContents() {
    final Set<AbstractResultDrop> result = Sea.filterDropsOfType(AbstractResultDrop.class, getDeponents());
    return result;
  }

  @Override
  public String getXMLElementName() {
    return "result-folder-drop";
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
