package com.surelogic.dropsea.irfree.drops;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisHintDrop;

public final class IRFreeAnalysisHintDrop extends IRFreeDrop implements IAnalysisHintDrop {

  private final HintType f_type;

  IRFreeAnalysisHintDrop(Entity e, Class<?> irClass) {
    this(e, irClass, null);
  }

  IRFreeAnalysisHintDrop(Entity e, Class<?> irClass, HintType forBackwardsCompatiblityOnly) {
    super(e, irClass);
    if (forBackwardsCompatiblityOnly != null) {
      f_type = forBackwardsCompatiblityOnly;
    } else {
      final String levelString = e.getAttribute(AbstractXMLReader.HINT_TYPE_ATTR);

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
  }

  @NonNull
  public HintType getHintType() {
    return f_type;
  }
}
