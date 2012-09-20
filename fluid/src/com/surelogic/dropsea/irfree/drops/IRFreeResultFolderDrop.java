package com.surelogic.dropsea.irfree.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;

public final class IRFreeResultFolderDrop extends IRFreeAnalysisResultDrop implements IResultFolderDrop {

  private final List<IRFreeResultFolderDrop> subFolders = new ArrayList<IRFreeResultFolderDrop>(0);
  private final List<IRFreeResultDrop> results = new ArrayList<IRFreeResultDrop>(0);
  private final FolderLogic f_operator;

  void addSubFolder(IRFreeResultFolderDrop info) {
    subFolders.add(info);
  }

  void addResult(IRFreeResultDrop info) {
    results.add(info);
  }

  IRFreeResultFolderDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    final String operatorString = e.getAttribute(AbstractXMLReader.FOLDER_LOGIC_OPERATOR);
    FolderLogic operator = FolderLogic.AND;
    if (operatorString != null) {
      try {
        operator = FolderLogic.valueOf(operatorString);
      } catch (Exception ignore) {
        // ignore
      }
    }
    f_operator = operator;
  }

  @NonNull
  public FolderLogic getFolderLogic() {
    return f_operator;
  }

  public Collection<? extends IResultDrop> getAnalysisResults() {
    return results;
  }

  public Collection<? extends IResultFolderDrop> getSubFolders() {
    return subFolders;
  }

  public Collection<? extends IAnalysisResultDrop> getContents() {
    Collection<IAnalysisResultDrop> rv = new HashSet<IAnalysisResultDrop>(results);
    rv.addAll(subFolders);
    return rv;
  }
}
