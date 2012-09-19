package com.surelogic.dropsea.irfree.drops;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisHintDrop;

public final class IRFreeAnalysisHintDrop extends IRFreeDrop implements IAnalysisHintDrop {

  private final HintType f_type;

  public IRFreeAnalysisHintDrop(Entity e) {
    this(e, null);
  }

  public IRFreeAnalysisHintDrop(Entity e, HintType forBackwardsCompatiblityOnly) {
    super(e);
    if (forBackwardsCompatiblityOnly != null) {
      f_type = forBackwardsCompatiblityOnly;
    } else {
      final String levelString = getEntity().getAttribute(AbstractXMLReader.HINT_TYPE_ATTR);

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

  // @Override
  @NonNull
  public HintType getHintType() {
    return f_type;
  }
}
