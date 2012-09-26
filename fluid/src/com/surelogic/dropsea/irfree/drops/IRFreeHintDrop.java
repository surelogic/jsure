package com.surelogic.dropsea.irfree.drops;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IHintDrop;

public final class IRFreeHintDrop extends IRFreeDrop implements IHintDrop {

  private final HintType f_type;

  IRFreeHintDrop(Entity e, Class<?> irClass) {
    this(e, irClass, null);
  }

  IRFreeHintDrop(Entity e, Class<?> irClass, HintType forBackwardsCompatiblityOnly) {
    super(e, irClass);
    if (forBackwardsCompatiblityOnly != null) {
      f_type = forBackwardsCompatiblityOnly;
    } else {
      final String levelString = e.getAttribute(AbstractXMLReader.HINT_TYPE_ATTR);

      HintType level = HintType.INFORMATION;
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