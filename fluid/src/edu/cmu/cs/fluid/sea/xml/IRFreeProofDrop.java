package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import org.xml.sax.Attributes;

import edu.cmu.cs.fluid.sea.IProofDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public abstract class IRFreeProofDrop extends IRFreeDrop implements IProofDrop {
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
    return "true".equals(getAttribute(PromiseDrop.FROM_SRC));
  }
}
