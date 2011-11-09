package com.surelogic.opgen.syntax;

import java.util.regex.Matcher;

/**
 * Represents $@foo:FooInfo, which delegates
 * implementation to sub-operators
 * 
 * @author chance
 */
public class AbstractAttribute extends Attribute {
  AbstractAttribute(int i, String t, Matcher m) {
    super(i, t, m);
  }
  
  @Override
  public boolean isAbstract() { 
    return true;
  }
}
