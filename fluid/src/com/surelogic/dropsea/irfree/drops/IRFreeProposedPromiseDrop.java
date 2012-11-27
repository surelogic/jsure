package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.Entity;

public final class IRFreeProposedPromiseDrop extends IRFreeDrop implements IProposedPromiseDrop {

  @NonNull
  private final IJavaRef f_assumptionRef;
  @NonNull
  private final Map<String, String> f_annoAttributes;
  @NonNull
  private final Map<String, String> f_replacedAttributes;
  private final String f_JavaAnnotation;
  private final String f_annotation;
  private final String f_contents;
  private final String f_replacedAnnotation;
  private final String f_replacedContents;
  @NonNull
  private final Origin f_origin;

  @Override
  boolean aliasTheMessage() {
	  return true;
  }
  
  void setAnnoAttributes(Map<String, String> value) {
    f_annoAttributes.clear();
    for(Map.Entry<String, String> e : value.entrySet()) {
    	if (FLAVOR_ATTR.equals(e.getKey())) {
    		continue;
    	}
    	f_annoAttributes.put(DeclUtil.aliasIfPossible(e.getKey()), e.getValue());
    }
  }

  void setReplacedAttributes(Map<String, String> value) {
    f_replacedAttributes.clear();
    for(Map.Entry<String, String> e : value.entrySet()) {
    	if (FLAVOR_ATTR.equals(e.getKey())) {
    		continue;
    	}
    	f_replacedAttributes.put(DeclUtil.aliasIfPossible(e.getKey()), e.getValue());
    }
  }

  IRFreeProposedPromiseDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

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

  private static Map<String,String> makeAttrs(String noFlag) {
	  if ("true".equals(noFlag)) {
		  return Collections.emptyMap();
	  } 
	  return new HashMap<String, String>(0);
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
