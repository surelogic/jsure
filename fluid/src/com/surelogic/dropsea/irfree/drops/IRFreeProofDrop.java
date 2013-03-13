package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DERIVED_FROM_WARNING_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROVED_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.USES_RED_DOT_ATTR;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IProofDrop;

public abstract class IRFreeProofDrop extends IRFreeDrop implements IProofDrop {

  private final boolean f_proofUsesRedDot;
  private final boolean f_provedConsistent;
  private final boolean f_derivedFromSrc;
  private final boolean f_derivedFromWarningHint;

  IRFreeProofDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
    f_proofUsesRedDot = "true".equals(e.getAttribute(USES_RED_DOT_ATTR));
    f_provedConsistent = "true".equals(e.getAttribute(PROVED_ATTR));
    f_derivedFromSrc = "true".equals(e.getAttribute(DERIVED_FROM_SRC_ATTR));
    f_derivedFromWarningHint = "true".equals(e.getAttribute(DERIVED_FROM_WARNING_ATTR));
  }

  @Override
  public final boolean proofUsesRedDot() {
    return f_proofUsesRedDot;
  }

  @Override
  public final boolean provedConsistent() {
    return f_provedConsistent;
  }

  @Override
  public final boolean derivedFromSrc() {
    return f_derivedFromSrc;
  }

  @Override
  public boolean derivedFromWarningHint() {
    return f_derivedFromWarningHint;
  }
}
