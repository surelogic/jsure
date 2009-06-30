/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/StandardRules.java,v 1.2 2007/09/27 19:30:44 chance Exp $*/
package com.surelogic.annotation.rules;

import com.surelogic.annotation.*;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class StandardRules extends AnnotationRules {
  private static final AnnotationRules instance = new StandardRules();
  private static final NullAnnotationParseRule[] rules = {
    new NullAnnotationParseRule("Param"),
    new NullAnnotationParseRule("Return"),
    new NullAnnotationParseRule("Exception"),
    new NullAnnotationParseRule("Throws"),
    new NullAnnotationParseRule("Author"),
    new NullAnnotationParseRule("Version"),
    new NullAnnotationParseRule("See"),
    new NullAnnotationParseRule("Since"),
    new NullAnnotationParseRule("Serial"),
    new NullAnnotationParseRule("SerialField"),
    new NullAnnotationParseRule("SerialData"),
    new NullAnnotationParseRule("Deprecated"),
  };

  public static AnnotationRules getInstance() {
    return instance;
  }
  
  private StandardRules() {
    // do nothing
  }
  
  @Override
  public void register(PromiseFramework fw) {
    for(NullAnnotationParseRule r : rules) {
      fw.registerParseDropRule(r);
    }
  }
}
