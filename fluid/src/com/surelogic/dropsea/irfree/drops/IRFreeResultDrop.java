package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CONSISTENT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.TIMEOUT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.VOUCHED;

import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.irfree.JSureEntity;

public final class IRFreeResultDrop extends IRFreeAnalysisResultDrop implements IResultDrop {

  private final boolean f_isConsistent;
  private final boolean f_isVouched;
  private final boolean f_isTimeout;

  IRFreeResultDrop(JSureEntity e, Class<?> irClass) {
    super(e, irClass);

    f_isConsistent = "true".equals(e.getAttribute(CONSISTENT));
    f_isVouched = "true".equals(e.getAttribute(VOUCHED));
    f_isTimeout = "true".equals(e.getAttribute(TIMEOUT));
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
