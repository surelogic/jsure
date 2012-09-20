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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
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

  private JavaDeclInfo f_fromInfo;
  private JavaDeclInfo f_targetInfo;
  private ISrcRef f_assumptionRef;
  private final Map<String, String> f_annoAttributes = new HashMap<String, String>();
  private final Map<String, String> f_replacedAttributes = new HashMap<String, String>();
  private final String f_JavaAnnotation;
  private final String f_annotation;
  private final String f_contents;
  private final String f_replacedAnnotation;
  private final String f_replacedContents;
  private final String f_targetProjectName;
  private final String f_fromProjectName;
  private final Origin f_origin;

  void setAssumptionRef(ISrcRef value) {
    f_assumptionRef = value;
  }

  void setAnnoAttributes(Map<String, String> value) {
    f_annoAttributes.clear();
    f_annoAttributes.putAll(value);
  }

  void setReplacedAttributes(Map<String, String> value) {
    f_replacedAttributes.clear();
    f_replacedAttributes.putAll(value);
  }

  IRFreeProposedPromiseDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    f_JavaAnnotation = e.getAttribute(JAVA_ANNOTATION);
    f_annotation = e.getAttribute(ANNOTATION_TYPE);
    f_contents = e.getAttribute(CONTENTS);
    f_replacedAnnotation = e.getAttribute(REPLACED_ANNO);
    f_replacedContents = e.getAttribute(REPLACED_CONTENTS);
    f_targetProjectName = e.getAttribute(TARGET_PROJECT);
    f_fromProjectName = e.getAttribute(FROM_PROJECT);

    final String origin = e.getAttribute(ORIGIN);
    Origin result = Origin.MODEL; // default
    if (origin != null) {
      try {
        result = Origin.valueOf(origin);
      } catch (Exception e1) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(249, origin, ORIGIN), e1);
      }
    }
    f_origin = result;
  }

  public Map<String, String> getAnnoAttributes() {
    return f_annoAttributes;
  }

  public Map<String, String> getReplacedAttributes() {
    return f_replacedAttributes;
  }

  public String getJavaAnnotation() {
    return f_JavaAnnotation;
  }

  public String getAnnotation() {
    return f_annotation;
  }

  public String getContents() {
    return f_contents;
  }

  public String getReplacedAnnotation() {
    return f_replacedAnnotation;
  }

  public String getReplacedContents() {
    return f_replacedContents;
  }

  public Origin getOrigin() {
    return f_origin;
  }

  public boolean isAbductivelyInferred() {
    final Origin origin = getOrigin();
    return origin != Origin.CODE;
  }

  public String getTargetProjectName() {
    return f_targetProjectName;
  }

  public String getFromProjectName() {
    return f_fromProjectName;
  }

  public ISrcRef getAssumptionRef() {
    return f_assumptionRef;
  }

  public IJavaDeclaration getFromInfo() {
    return f_fromInfo.makeDecl();
  }

  public IJavaDeclaration getTargetInfo() {
    return f_targetInfo.makeDecl();
  }

  public void addInfo(JavaDeclInfo info) {
    String flavor = info.getAttribute(FLAVOR_ATTR);
    // System.out.println("addInfo " + flavor +
    // " called on proposed promise drop " + this + " : " + info);
    if (FROM_INFO.equals(flavor)) {
      f_fromInfo = info;
    } else if (TARGET_INFO.equals(flavor)) {
      f_targetInfo = info;
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
