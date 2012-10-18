package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNOTATION_TYPE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTENTS;
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

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;

public final class IRFreeProposedPromiseDrop extends IRFreeDrop implements IProposedPromiseDrop {

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

  @Nullable
  private IJavaRef f_assumptionRef = null;
  @NonNull
  private final Map<String, String> f_annoAttributes = new HashMap<String, String>(0);
  @NonNull
  private final Map<String, String> f_replacedAttributes = new HashMap<String, String>(0);
  private final String f_JavaAnnotation;
  private final String f_annotation;
  private final String f_contents;
  private final String f_replacedAnnotation;
  private final String f_replacedContents;
  private final String f_targetProjectName;
  private final String f_fromProjectName;
  @NonNull
  private final Origin f_origin;

  void setAssumptionRef(IJavaRef value) {
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
        // FIX
        if ("PROMISE".equals(origin)) {
          result = Origin.MODEL;
        } else if ("INFERENCE".equals(origin)) {
          result = Origin.CODE;
        } else {
          result = Origin.valueOf(origin);
        }
      } catch (Exception e1) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(259, origin, ORIGIN), e1);
      }
    }
    f_origin = result;

    final String encodedJavaRef = e.getAttribute(FROM_REF);
    if (encodedJavaRef != null) {
      try {
        final IJavaRef ref = JavaRef.parseEncodedForPersistence(encodedJavaRef);
        f_assumptionRef = ref;
      } catch (Exception parseFailure) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(288, encodedJavaRef), parseFailure);
      }
    }
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

  public IJavaRef getAssumptionRef() {
    return f_assumptionRef;
  }

  boolean hasAssumptionRef() {
    return f_assumptionRef != null;
  }

  public static boolean isSameProposalAs(IProposedPromiseDrop o1, IProposedPromiseDrop o2) {
    if (o1 == null && o2 == null)
      return true;
    if (o1 == null && o2 != null)
      return false;
    if (o1 != null && o2 == null)
      return false;

    return isSame(o1.getAnnotation(), o2.getAnnotation()) && isSame(o1.getContents(), o2.getContents())
        && isSame(o1.getReplacedContents(), o2.getReplacedContents()) && isSame(o1.getJavaRef(), o2.getJavaRef());
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
}
