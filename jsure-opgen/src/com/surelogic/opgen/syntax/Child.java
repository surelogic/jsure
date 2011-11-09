package com.surelogic.opgen.syntax;

import java.util.regex.Matcher;

/**
 * Represents foo:FooOperator
 * 
 * @author chance
 */
public class Child extends SyntaxElement {
  static Child create(int i, String word, Matcher m) {
    if (word.startsWith("@")) {
      return new AbstractChild(i, word, m);
    } else {
      return new Child(i, word, m);
    }
  }
  
  public final String name;
  public final String type;
  public final String prec;
  public final boolean opt;
  public final int childNum;
  
  Child(int i, String t, Matcher m) {
    super(i, t);    
    String name = m.group(1);
    if (name == null) {
      // Default to child1, child2, etc.
      name = "child"+i;
    } else { // get rid of :
      name = name.substring(0, name.length()-1);
    }      
    this.name = name;
    this.childNum = i;
    
    boolean isOptional = false;
    String type = m.group(2);
    if (type.endsWith("?")) {
      isOptional = true;
      type       = type.substring(0, type.length()-1);
    }
    this.opt  = isOptional;
    this.type = type;
    this.prec = m.group(3);
  }
  
  public boolean hasPrecedence() {
    return prec != null;
  }
  
  public boolean isAbstract() { 
    return false;
  }
  
  @Override
  public String toString() {
    final String clazz = this.getClass().getName();
    if (prec == null) {
      return clazz+" "+index+": "+name+" : "+type;
    }
    return clazz+" "+index+": "+name+" : "+type+" "+prec;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof Child) {
      Child c2 = (Child) o;
      if (name.equals(c2.name) && type.equals(c2.type)) {        
        return (prec == null)? (c2.prec == null) : prec.equals(c2.prec);
      }
    }
    return false;
  }
  @Override
  public int hashCode() {
    return name.hashCode() + type.hashCode();
  }
}
