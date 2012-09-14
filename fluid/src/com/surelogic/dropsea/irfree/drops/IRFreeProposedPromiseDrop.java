package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.FLAVOR_ATTR;
import static com.surelogic.common.jsure.xml.JSureXMLReader.PROPERTIES;
import static com.surelogic.common.jsure.xml.JSureXMLReader.SOURCE_REF;

import java.util.Map;

import org.xml.sax.Attributes;

import com.surelogic.common.refactor.IJavaDeclInfoClient;
import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.common.refactor.JavaDeclInfo;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;

import edu.cmu.cs.fluid.java.ISrcRef;

public final class IRFreeProposedPromiseDrop extends IRFreeDrop implements IProposedPromiseDrop, IJavaDeclInfoClient,
    Comparable<IRFreeProposedPromiseDrop> {
  static {
    internString(ProposedPromiseDrop.FROM_INFO);
    internString(ProposedPromiseDrop.TARGET_INFO);
    internString(ProposedPromiseDrop.FROM_REF);
    internString(ProposedPromiseDrop.class.getName());
    internString("ProposedPromiseDrop @RegionEffects(writes java.lang.Object:All)");
    internString("@RegionEffects(writes java.lang.Object:All)");
    internString("ProposedPromiseDrop");
    internString("RegionEffects");
    internString("writes java.lang.Object:All");
    internString("ProposedPromiseDrop @RegionEffects(reads this:Instance)");
    internString("ProposedPromiseDrop @RegionEffects(none)");
    internString("@RegionEffects(reads this:Instance)");
    internString("@RegionEffects(none)");
  }

  private JavaDeclInfo fromInfo;
  private JavaDeclInfo targetInfo;
  private ISrcRef assumptionRef;
  private Map<String, String> annoAttrs, replacedAttrs;

  public IRFreeProposedPromiseDrop(String name, Attributes a) {
    super(name, a);
  }

  public Map<String, String> getAnnoAttributes() {
    return annoAttrs;
  }

  public Map<String, String> getReplacedAttributes() {
    return replacedAttrs;
  }

  public String getJavaAnnotation() {
    return getAttribute(ProposedPromiseDrop.JAVA_ANNOTATION);
  }

  public String getAnnotation() {
    return getAttribute(ProposedPromiseDrop.ANNOTATION_TYPE);
  }

  public String getContents() {
    return getAttribute(ProposedPromiseDrop.CONTENTS);
  }

  public String getReplacedAnnotation() {
    return getAttribute(ProposedPromiseDrop.REPLACED_ANNO);
  }

  public String getReplacedContents() {
    return getAttribute(ProposedPromiseDrop.REPLACED_CONTENTS);
  }

  public Origin getOrigin() {
    final String origin = getAttribute(ProposedPromiseDrop.ORIGIN);
    Origin result = Origin.MODEL;
    if (origin == null) {
      /*
       * The scan is old and doesn't have an origin, just return a default.
       */
      return result;
    }
    try {
      result = Origin.valueOf(origin);
    } catch (Exception ignoreTakeDefault) {
      // Ignore we set a default
    }
    return result;
  }

  public boolean isAbductivelyInferred() {
    final Origin origin = getOrigin();
    return origin != Origin.CODE;
  }

  public String getTargetProjectName() {
    return getAttribute(ProposedPromiseDrop.TARGET_PROJECT);
  }

  public String getFromProjectName() {
    return getAttribute(ProposedPromiseDrop.FROM_PROJECT);
  }

  public ISrcRef getAssumptionRef() {
    return assumptionRef;
  }

  public IJavaDeclaration getFromInfo() {
    return fromInfo.makeDecl();
  }

  public IJavaDeclaration getTargetInfo() {
    return targetInfo.makeDecl();
  }

  public void addInfo(JavaDeclInfo info) {
    String flavor = info.getAttribute(FLAVOR_ATTR);
    if (ProposedPromiseDrop.FROM_INFO.equals(flavor)) {
      fromInfo = info;
    } else if (ProposedPromiseDrop.TARGET_INFO.equals(flavor)) {
      targetInfo = info;
    } else {
      throw new IllegalStateException("Unknown flavor of info: " + flavor);
    }
  }

  @Override
  public void addRef(Entity e) {
    final String name = e.getName();
    if (SOURCE_REF.equals(name)) {
      SourceRef sr = new SourceRef(e);
      if (ProposedPromiseDrop.FROM_REF.equals(e.getAttribute(FLAVOR_ATTR))) {
        assumptionRef = makeSrcRef(sr);
      } else {
        setSource(sr);
      }
    } else if (PROPERTIES.equals(name)) {
      if (ProposedPromiseDrop.ANNO_ATTRS.equals(e.getAttribute(FLAVOR_ATTR))) {
        annoAttrs = e.getAttributes();
      } else {
        replacedAttrs = e.getAttributes();
      }
    } else {
      super.addRef(e);
    }
  }

  public boolean isSameProposalAs(IProposedPromiseDrop other) {
    if (this == other)
      return true;
    if (other == null)
      return false;

    return isSame(getAnnotation(), other.getAnnotation()) && isSame(getContents(), other.getContents())
        && isSame(getReplacedContents(), other.getReplacedContents()) && isSame(getSrcRef(), other.getSrcRef());
  }

  private static <T> boolean isSame(T o1, T o2) {
    if (o1 == null) {
      if (o2 != null) {
        return false;
      }
    } else if (!o1.equals(o2)) {
      return false;
    }
    return true;
  }

  public long computeHash() {
    long hash = 0;
    final String anno = getAnnotation();
    if (anno != null) {
      hash += anno.hashCode();
    }
    final String contents = getContents();
    if (contents != null) {
      hash += contents.hashCode();
    }
    final String replaced = getReplacedContents();
    if (replaced != null) {
      hash += replaced.hashCode();
    }
    final ISrcRef ref = getSrcRef();
    if (ref != null) {
      hash += ref.getHash(); // Instead of hashCode()?
    }
    return hash;
  }

  public int compareTo(IRFreeProposedPromiseDrop o) {
    return getMessage().compareTo(o.getMessage());
  }
}
