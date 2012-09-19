package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNOTATION_TYPE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTENTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FLAVOR_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_INFO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_PROJECT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_REF;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_ANNOTATION;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.ORIGIN;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_ANNO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_CONTENTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TARGET_INFO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TARGET_PROJECT;

import java.util.Map;

import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.common.refactor.JavaDeclInfo;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;

import edu.cmu.cs.fluid.java.ISrcRef;

public final class IRFreeProposedPromiseDrop extends IRFreeDrop implements IProposedPromiseDrop,
    Comparable<IRFreeProposedPromiseDrop> {

  static {
    Entity.internString(FROM_INFO);
    Entity.internString(TARGET_INFO);
    Entity.internString(FROM_REF);
    Entity.internString(ProposedPromiseDrop.class.getName());
    Entity.internString("ProposedPromiseDrop @RegionEffects(writes java.lang.Object:All)");
    Entity.internString("@RegionEffects(writes java.lang.Object:All)");
    Entity.internString("ProposedPromiseDrop");
    Entity.internString("RegionEffects");
    Entity.internString("writes java.lang.Object:All");
    Entity.internString("ProposedPromiseDrop @RegionEffects(reads this:Instance)");
    Entity.internString("ProposedPromiseDrop @RegionEffects(none)");
    Entity.internString("@RegionEffects(reads this:Instance)");
    Entity.internString("@RegionEffects(none)");
  }

  private JavaDeclInfo fromInfo;
  private JavaDeclInfo targetInfo;
  // TODO
  public ISrcRef assumptionRef;
  public Map<String, String> annoAttrs, replacedAttrs;

  public IRFreeProposedPromiseDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
  }

  public Map<String, String> getAnnoAttributes() {
    return annoAttrs;
  }

  public Map<String, String> getReplacedAttributes() {
    return replacedAttrs;
  }

  public String getJavaAnnotation() {
    return getEntity().getAttribute(JAVA_ANNOTATION);
  }

  public String getAnnotation() {
    return getEntity().getAttribute(ANNOTATION_TYPE);
  }

  public String getContents() {
    return getEntity().getAttribute(CONTENTS);
  }

  public String getReplacedAnnotation() {
    return getEntity().getAttribute(REPLACED_ANNO);
  }

  public String getReplacedContents() {
    return getEntity().getAttribute(REPLACED_CONTENTS);
  }

  public Origin getOrigin() {
    final String origin = getEntity().getAttribute(ORIGIN);
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
    return getEntity().getAttribute(TARGET_PROJECT);
  }

  public String getFromProjectName() {
    return getEntity().getAttribute(FROM_PROJECT);
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
    // System.out.println("addInfo " + flavor +
    // " called on proposed promise drop " + this + " : " + info);
    if (FROM_INFO.equals(flavor)) {
      fromInfo = info;
    } else if (TARGET_INFO.equals(flavor)) {
      targetInfo = info;
    } else {
      throw new IllegalStateException("Unknown flavor of info: " + flavor);
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
