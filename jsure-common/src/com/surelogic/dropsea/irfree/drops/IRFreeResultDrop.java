package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CONSISTENT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.TIMEOUT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.VOUCHED;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IResultDrop;

public final class IRFreeResultDrop extends IRFreeAnalysisResultDrop implements IResultDrop {

  private final boolean f_isConsistent;
  private final boolean f_isVouched;
  private final boolean f_isTimeout;

  IRFreeResultDrop(Entity e) {
    super(e);

    f_isConsistent = "true".equals(e.getAttribute(CONSISTENT));
    f_isVouched = "true".equals(e.getAttribute(VOUCHED));
    f_isTimeout = "true".equals(e.getAttribute(TIMEOUT));
  }

  @NonNull
  @Override
  public final DropType getDropType() {
    return DropType.RESULT;
  }

  @Override
  boolean aliasTheMessage() {
    return true;
  }

  @Override
  public boolean isConsistent() {
    return f_isConsistent;
  }

  @Override
  public boolean isVouched() {
    return f_isVouched;
  }

  @Override
  public boolean isTimeout() {
    return f_isTimeout;
  }
}
