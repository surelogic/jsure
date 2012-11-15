package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ANNOTATION_TYPE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CONTENTS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FROM_REF;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.JAVA_ANNOTATION;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ORIGIN;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_ANNO;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_CONTENTS;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.Entity;

public final class IRFreeProposedPromiseDrop extends IRFreeDrop implements IProposedPromiseDrop {

  @NonNull
  private final IJavaRef f_assumptionRef;
  @NonNull
  private final Map<String, String> f_annoAttributes = new HashMap<String, String>(0);
  @NonNull
  private final Map<String, String> f_replacedAttributes = new HashMap<String, String>(0);
  private final String f_JavaAnnotation;
  private final String f_annotation;
  private final String f_contents;
  private final String f_replacedAnnotation;
  private final String f_replacedContents;
  @NonNull
  private final Origin f_origin;

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
    if (encodedJavaRef == null)
      throw new IllegalStateException(I18N.err(288, encodedJavaRef));
    try {
      final IJavaRef ref = e.parsePersistedRef(encodedJavaRef);
      f_assumptionRef = ref;
    } catch (Exception parseFailure) {
      throw new IllegalStateException(I18N.err(288, encodedJavaRef), parseFailure);
    }

    // Java reference must be non-null for a proposed promise
    if (getJavaRef() == null)
      throw new IllegalStateException(I18N.err(44, "getJavaRef()"));
  }

  @NonNull
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

  public IJavaRef getAssumptionRef() {
    return f_assumptionRef;
  }

  boolean hasAssumptionRef() {
    return f_assumptionRef != null;
  }
}
