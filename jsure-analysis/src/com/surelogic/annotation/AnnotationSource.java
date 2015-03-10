/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/AnnotationSource.java,v 1.3 2007/07/13 14:01:58 chance Exp $*/
package com.surelogic.annotation;

/**
 * Describes where an annotation is from
 * 
 * @author Edwin.Chan
 */
public enum AnnotationSource {
  JAVA_5(true), JAVADOC(true), XML(false);
  
  private final boolean fromSrc;
  
  private AnnotationSource(boolean src) {
    fromSrc = src;
  }
  
  public boolean isFromSource() {
    return fromSrc;
  }
}
