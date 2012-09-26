package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.*;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_SRC;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IProofDrop;

public abstract class IRFreeProofDrop extends IRFreeDrop implements IProofDrop {

  private final boolean f_proofUsesRedDot;
  private final boolean f_provedConsistent;
  private final boolean f_derivedFromSrc;
  private final boolean f_isFromSrc;
  private final boolean f_derivedFromWarningHint;

  IRFreeProofDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
    f_proofUsesRedDot = "true".equals(e.getAttribute(USES_RED_DOT_ATTR));
    f_provedConsistent = "true".equals(e.getAttribute(PROVED_ATTR));
    f_derivedFromSrc = "true".equals(e.getAttribute(DERIVED_FROM_SRC_ATTR));
    f_derivedFromWarningHint = "true".equals(e.getAttribute(DERIVED_FROM_WARNING_ATTR));
    f_isFromSrc = "true".equals(e.getAttribute(FROM_SRC));
  }

  public final boolean proofUsesRedDot() {
    return f_proofUsesRedDot;
  }

  public final boolean provedConsistent() {
    return f_provedConsistent;
  }

  public final boolean derivedFromSrc() {
    return f_derivedFromSrc;
  }

  public boolean derivedFromWarningHint() {
    return f_derivedFromWarningHint;
  }

  public final boolean isFromSrc() {
    return f_isFromSrc;
  }
}
