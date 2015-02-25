package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ANNOTATION_TYPE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CONTENTS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FLAVOR_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FROM_REF;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.JAVA_ANNOTATION;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.NO_ANNO_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.NO_REPLACED_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ORIGIN;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_ANNO;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_CONTENTS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IProposedPromiseDrop;

public final class IRFreeProposedPromiseDrop extends IRFreeDrop implements IProposedPromiseDrop {

  @NonNull
  private final IJavaRef f_assumptionRef;
  @NonNull
  private final Map<String, String> f_annoAttributes;
  @NonNull
  private final Map<String, String> f_replacedAttributes;
  private final String f_JavaAnnotation;
  @NonNull
  private final String f_annotation;
  private final String f_contents;
  private final String f_replacedAnnotation;
  private final String f_replacedContents;
  @NonNull
  private final Origin f_origin;

  public final DropType getDropType() {
	return DropType.PROPOSAL;
  }
  
  @Override
  boolean aliasTheMessage() {
    return true;
  }

  void setAnnoAttributes(Map<String, String> value) {
    f_annoAttributes.clear();
    for (Map.Entry<String, String> e : value.entrySet()) {
      if (FLAVOR_ATTR.equals(e.getKey())) {
        continue;
      }
      f_annoAttributes.put(DeclUtil.aliasIfPossible(e.getKey()), e.getValue());
    }
  }

  void setReplacedAttributes(Map<String, String> value) {
    f_replacedAttributes.clear();
    for (Map.Entry<String, String> e : value.entrySet()) {
      if (FLAVOR_ATTR.equals(e.getKey())) {
        continue;
      }
      f_replacedAttributes.put(DeclUtil.aliasIfPossible(e.getKey()), e.getValue());
    }
  }

  IRFreeProposedPromiseDrop(Entity e) {
    super(e);

    f_JavaAnnotation = e.getAttributeByAliasIfPossible(JAVA_ANNOTATION);
    f_annotation = e.getAttributeByAliasIfPossible(ANNOTATION_TYPE);
    f_contents = e.getAttributeByAliasIfPossible(CONTENTS);
    f_replacedAnnotation = e.getAttributeByAliasIfPossible(REPLACED_ANNO);
    f_replacedContents = e.getAttributeByAliasIfPossible(REPLACED_CONTENTS);

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

    f_annoAttributes = makeAttrs(e.getAttribute(NO_ANNO_ATTRS));
    f_replacedAttributes = makeAttrs(e.getAttribute(NO_REPLACED_ATTRS));
  }

  private static Map<String, String> makeAttrs(String noFlag) {
    if ("true".equals(noFlag)) {
      return Collections.emptyMap();
    }
    return new HashMap<String, String>(0);
  }

  @Override
  @NonNull
  public String getJavaAnnotation() {
    return f_JavaAnnotation;
  }

  @Override
  @NonNull
  public String getAnnotation() {
    return f_annotation;
  }

  @Override
  @Nullable
  public String getValue() {
    return f_contents;
  }

  @Override
  @NonNull
  public Map<String, String> getAttributes() {
    return f_annoAttributes;
  }

  @Override
  @Nullable
  public String getReplacedAnnotation() {
    return f_replacedAnnotation;
  }

  @Override
  @Nullable
  public String getReplacedValue() {
    return f_replacedContents;
  }

  @Override
  @NonNull
  public Map<String, String> getReplacedAttributes() {
    return f_replacedAttributes;
  }

  @Override
  @NonNull
  public IJavaRef getAssumptionRef() {
    return f_assumptionRef;
  }

  @Override
  @NonNull
  public Origin getOrigin() {
    return f_origin;
  }

  @Override
  public boolean isAbductivelyInferred() {
    final Origin origin = getOrigin();
    return origin != Origin.CODE;
  }
}
