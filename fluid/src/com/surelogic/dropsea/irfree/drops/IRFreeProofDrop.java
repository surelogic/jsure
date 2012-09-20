package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_SRC;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IProofDrop;

public abstract class IRFreeProofDrop extends IRFreeDrop implements IProofDrop {

  private final List<IRFreeAnalysisHintDrop> f_analysisHints = new ArrayList<IRFreeAnalysisHintDrop>(0);
  private final boolean f_proofUsesRedDot;
  private final boolean f_provedConsistent;
  private final boolean f_derivedFromSrc;
  private final boolean f_isFromSrc;

  void addAnalysisHint(IRFreeAnalysisHintDrop hint) {
    f_analysisHints.add(hint);
  }

  IRFreeProofDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
    f_proofUsesRedDot = "true".equals(e.getAttribute(USES_RED_DOT_ATTR));
    f_provedConsistent = "true".equals(e.getAttribute(PROVED_ATTR));
    f_derivedFromSrc = "true".equals(e.getAttribute(DERIVED_FROM_SRC_ATTR));
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

  public final boolean isFromSrc() {
    return f_isFromSrc;
  }

  @NonNull
  public final Collection<? extends IAnalysisHintDrop> getAnalysisHintsAbout() {
    return f_analysisHints;
  }
}
