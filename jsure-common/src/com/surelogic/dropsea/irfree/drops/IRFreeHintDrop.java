package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.HINT_TYPE_ATTR;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IHintDrop;

public final class IRFreeHintDrop extends IRFreeDrop implements IHintDrop {

  private final HintType f_type;

  IRFreeHintDrop(Entity e) {
    this(e, null);
  }

  IRFreeHintDrop(Entity e, HintType forBackwardsCompatiblityOnly) {
    super(e);
    if (forBackwardsCompatiblityOnly != null) {
      f_type = forBackwardsCompatiblityOnly;
    } else {
      final String levelString = e.getAttribute(HINT_TYPE_ATTR);

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
  @Override
  public final DropType getDropType() {
    return DropType.HINT;
  }

  @Override
  boolean aliasTheMessage() {
    return true;
  }

  @Override
  @NonNull
  public HintType getHintType() {
    return f_type;
  }
}
