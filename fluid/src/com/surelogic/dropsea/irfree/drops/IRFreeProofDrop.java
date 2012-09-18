package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_SRC;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xml.sax.Attributes;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IProofDrop;

public abstract class IRFreeProofDrop extends IRFreeDrop implements IProofDrop {

  private final List<IRFreeAnalysisHintDrop> analysisHints = new ArrayList<IRFreeAnalysisHintDrop>(0);

  public void addAnalysisHint(IRFreeAnalysisHintDrop hint) {
    analysisHints.add(hint);
  }

  IRFreeProofDrop(String name, Attributes a) {
    super(name, a);
  }

  public final boolean proofUsesRedDot() {
    return "true".equals(getAttribute(USES_RED_DOT_ATTR));
  }

  public final boolean provedConsistent() {
    return "true".equals(getAttribute(PROVED_ATTR));
  }

  public final boolean derivedFromSrc() {
    return "true".equals(getAttribute(DERIVED_FROM_SRC_ATTR));
  }

  public final boolean isFromSrc() {
    return "true".equals(getAttribute(FROM_SRC));
  }

  @NonNull
  public Collection<? extends IAnalysisHintDrop> getAnalysisHintsAbout() {
    return analysisHints;
  }
}
