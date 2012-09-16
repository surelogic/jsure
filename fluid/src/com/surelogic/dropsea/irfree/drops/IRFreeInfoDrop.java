package com.surelogic.dropsea.irfree.drops;

import org.xml.sax.Attributes;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.dropsea.IAnalysisHintDrop;

public final class IRFreeInfoDrop extends IRFreeDrop implements IAnalysisHintDrop {

  private final HintType f_type;

  public IRFreeInfoDrop(String name, Attributes a) {
    super(name, a);
    final String levelString = getAttribute(AbstractXMLReader.HINT_TYPE_ATTR);

    HintType level = HintType.SUGGESTION;
    if (levelString != null) {
      try {
        level = HintType.valueOf(levelString);
      } catch (Exception ignore) {
        // ignore
      }
    }
    f_type = level;
  }

  @Override
  @NonNull
  public HintType getLevel() {
    return f_type;
  }
}
