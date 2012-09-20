package com.surelogic.dropsea.irfree.drops;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IResultFolderDrop;

public final class IRFreeResultFolderDrop extends IRFreeAnalysisResultDrop implements IResultFolderDrop {

  private final FolderLogic f_operator;

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
}
