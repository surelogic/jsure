package com.surelogic.dropsea.irfree.drops;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IResultFolderDrop;

public final class IRFreeResultFolderDrop extends IRFreeAnalysisResultDrop implements IResultFolderDrop {

  private final LogicOperator f_operator;

  IRFreeResultFolderDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    final String operatorString = e.getAttribute(AbstractXMLReader.FOLDER_LOGIC_OPERATOR);
    LogicOperator operator = LogicOperator.AND;
    if (operatorString != null) {
      try {
        operator = LogicOperator.valueOf(operatorString);
      } catch (Exception ignore) {
        // ignore
      }
    }
    f_operator = operator;
  }

  @NonNull
  public LogicOperator getLogicOperator() {
    return f_operator;
  }
}
