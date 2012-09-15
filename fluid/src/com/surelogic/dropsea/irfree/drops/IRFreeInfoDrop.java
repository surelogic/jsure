package com.surelogic.dropsea.irfree.drops;

import org.xml.sax.Attributes;

import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.dropsea.IInfoDrop;
import com.surelogic.dropsea.InfoDropLevel;

public final class IRFreeInfoDrop extends IRFreeDrop implements IInfoDrop {

  private final InfoDropLevel f_level;

  public IRFreeInfoDrop(String name, Attributes a) {
    super(name, a);
    final String levelString = getAttribute(AbstractXMLReader.INFO_LEVEL_ATTR);

    InfoDropLevel level = InfoDropLevel.INFORMATION;
    if (levelString != null) {
      try {
        level = InfoDropLevel.valueOf(levelString);
      } catch (Exception ignore) {
        // ignore
      }
    }
    f_level = level;
  }

  @Override
  @NonNull
  public InfoDropLevel getLevel() {
    return f_level;
  }
}
