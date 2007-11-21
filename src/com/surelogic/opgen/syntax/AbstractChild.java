package com.surelogic.opgen.syntax;

import java.util.regex.Matcher;

/**
 * Represents @foo:FooChild, which delegates
 * implementation to sub-operators
 * 
 * @author chance
 */
public class AbstractChild extends Child {
  AbstractChild(int i, String t, Matcher m) {
    super(i, t, m);
  }
  
  @Override
  public boolean isAbstract() { 
    return true;
  }
}
