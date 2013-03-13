package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FOLDER_LOGIC_OPERATOR;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IResultFolderDrop;

public final class IRFreeResultFolderDrop extends IRFreeAnalysisResultDrop implements IResultFolderDrop {

  private final LogicOperator f_operator;

  IRFreeResultFolderDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    final String operatorString = e.getAttribute(FOLDER_LOGIC_OPERATOR);
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

  @Override
  @NonNull
  public LogicOperator getLogicOperator() {
    return f_operator;
  }
  
  @Override
  boolean aliasTheMessage() {
	  return true;
  }
}
