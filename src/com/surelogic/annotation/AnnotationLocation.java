/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/AnnotationLocation.java,v 1.3 2007/06/25 21:21:20 chance Exp $*/
package com.surelogic.annotation;

import com.surelogic.annotation.parse.SLAnnotationsParser;

/**
 * A standard way to specify how an AAST should be linked to FAST
 *
 * @author Edwin.Chan
 */
public enum AnnotationLocation {
  DECL, 
  RECEIVER, 
  RETURN_VAL,
  PARAMETER;
  
  public static AnnotationLocation translateTokenType(int type) {
    switch (type) {
      case SLAnnotationsParser.Nothing:
        return AnnotationLocation.DECL;
      case SLAnnotationsParser.ThisExpression:
        return AnnotationLocation.RECEIVER;
      case SLAnnotationsParser.ReturnValueDeclaration:
        return AnnotationLocation.RETURN_VAL;
      case SLAnnotationsParser.VariableUseExpression:
        return AnnotationLocation.PARAMETER;      
      default:
        throw new IllegalArgumentException("Unexpected type: "+SLAnnotationsParser.tokenNames[type]);
    }
  }
}
