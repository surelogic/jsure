package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONSISTENT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TIMEOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VOUCHED;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IResultDrop;

public final class IRFreeResultDrop extends IRFreeAnalysisResultDrop implements IResultDrop {

  private final boolean f_isConsistent;
  private final boolean f_isVouched;
  private final boolean f_isTimeout;

  IRFreeResultDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    f_isConsistent = "true".equals(e.getAttribute(CONSISTENT));
    f_isVouched = "true".equals(e.getAttribute(VOUCHED));
    f_isTimeout = "true".equals(e.getAttribute(TIMEOUT));
  }

  public boolean isConsistent() {
    return f_isConsistent;
  }

  public boolean isVouched() {
    return f_isVouched;
  }

  public boolean isTimeout() {
    return f_isTimeout;
  }
}
